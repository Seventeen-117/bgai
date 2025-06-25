# 令牌过期验证的问题与修复方案

## 问题描述

在系统中发现一个与令牌验证相关的问题：当通过 `/api/auth/refresh-by-userid?userId=<ID>` API 刷新用户的 token 后，使用该用户的旧 token 仍然可以成功调用 `/api/chatGatWay-internal` API，这导致了潜在的安全风险。

具体来说：
1. 当通过 `/api/auth/refresh-by-userid?userId=689258T` 刷新用户 token 后
2. 旧 token 应该被视为无效
3. 但使用旧 token 调用 `/api/chatGatWay-internal` API 仍然成功（服务未重启时）
4. 服务重启后，使用旧 token 调用 `/api/chatGatWay-internal` 会正确报错："Authorization token的用户ID与X-User-Id不匹配"

## 问题根本原因

1. **Redis缓存清理不彻底**：
   - 在刷新令牌流程中，只删除了数据库中找到的旧令牌的缓存
   - 由于多种原因（数据库事务延迟、代码逻辑顺序等），可能没有彻底清理旧令牌的所有Redis缓存引用

2. **Redis与数据库同步问题**：
   - 令牌先在数据库中更新，然后才清理缓存
   - 可能存在数据库更新完成但缓存尚未清理的时间窗口

3. **缓存持久化**：
   - 服务不重启时，一些令牌信息可能保存在内存缓存中
   - 重启后这些缓存被清除，使得系统默认从数据库验证，从而展现出正确行为

## 修复方案

我们对以下几个关键部分进行了修改：

### 1. `UserServiceImpl.refreshTokenByUserId` 方法

```java
@Override
@Transactional
@DS("master")
public UserToken refreshTokenByUserId(String userId) {
    // 保存旧令牌，确保稍后可以删除其缓存
    String oldAccessToken = user.getAccessToken();
    log.info("用户[{}]的旧令牌: {}", userId, oldAccessToken);
    
    // ... 原有代码 ...
    
    UserToken newToken = refreshToken(refreshToken);
    
    // 确保删除了旧令牌的所有缓存
    if (oldAccessToken != null && !oldAccessToken.isEmpty()) {
        String oldTokenKey = TOKEN_KEY_PREFIX + oldAccessToken;
        log.info("显式删除旧令牌的Redis缓存: {}", oldTokenKey);
        userTokenRedisTemplate.delete(oldTokenKey);
    }
    
    return newToken;
}
```

### 2. `UserServiceImpl.refreshToken` 方法

```java
@Override
@Transactional
@DS("master")
public UserToken refreshToken(String refreshToken) {
    // 查找拥有此刷新令牌的用户
    User userWithRefreshToken = userMapper.findByRefreshToken(refreshToken);
    String oldAccessToken = null;
    String userId = null;
    
    if (userWithRefreshToken != null) {
        // 保存旧令牌和用户ID，以便稍后清理缓存
        oldAccessToken = userWithRefreshToken.getAccessToken();
        userId = userWithRefreshToken.getUserId();
        log.info("找到拥有刷新令牌的用户: {}，原访问令牌: {}", userId, oldAccessToken);
    }
    
    // ... 原有代码 ...
    
    // 如果之前没有确定旧令牌，现在从数据库中获取
    if (oldAccessToken == null) {
        oldAccessToken = user.getAccessToken();
        log.info("从数据库获取用户[{}]的旧令牌: {}", userId, oldAccessToken);
    }
    
    // ... 原有代码 ...
    
    // 删除旧令牌
    if (oldAccessToken != null && !oldAccessToken.isEmpty()) {
        String oldTokenKey = TOKEN_KEY_PREFIX + oldAccessToken;
        log.info("删除旧令牌的Redis缓存: {}", oldTokenKey);
        userTokenRedisTemplate.delete(oldTokenKey);
    }
    
    log.info("成功刷新用户[{}]的令牌，旧令牌：{}，新令牌：{}", userId, oldAccessToken, newAccessToken);
}
```

### 3. 添加 `UserMapper.findByRefreshToken` 方法

```java
/**
 * 根据刷新令牌查询用户
 * 
 * @param refreshToken 刷新令牌
 * @return 用户实体
 */
@Select("SELECT * FROM t_user WHERE refresh_token = #{refreshToken}")
User findByRefreshToken(@Param("refreshToken") String refreshToken);
```

## 测试方法

1. 获取用户令牌：
   ```
   curl -X POST http://localhost:8688/api/auth/refresh-by-userid?userId=689258T \
        -H "Authorization: Bearer 管理员令牌"
   ```

2. 使用新令牌测试：
   ```
   curl -X POST http://localhost:8688/api/chatGatWay-internal \
        -H "Authorization: Bearer 新令牌" \
        -H "X-User-Id: 689258T" \
        -F "question=测试问题"
   ```

3. 使用旧令牌测试（应该失败）：
   ```
   curl -X POST http://localhost:8688/api/chatGatWay-internal \
        -H "Authorization: Bearer 旧令牌" \
        -H "X-User-Id: 689258T" \
        -F "question=测试问题"
   ```

## 额外建议

1. 考虑使用分布式锁来确保令牌刷新操作的原子性
2. 添加额外的健壮性检查，例如在每次API请求时验证令牌的有效性和最新性
3. 定期运行后台任务清理过期或无效的令牌缓存
4. 使用Redis发布/订阅机制来通知所有服务实例令牌已被刷新
5. 在Redis中使用更复杂的数据结构来维护令牌与用户的映射关系 