# 接口签名验证功能说明

## 概述

本项目实现了完整的接口签名验证功能，包括HMAC-SHA256签名验证、时间戳验证和nonce防重放攻击。该功能可以有效防止接口被恶意调用、重放攻击和数据篡改。

## 功能特性

### 1. 签名算法
- **算法**: HMAC-SHA256
- **密钥**: 每个应用有独立的密钥
- **签名格式**: 十六进制字符串

### 2. 安全机制
- **时间戳验证**: 防止过期请求（默认5分钟有效期）
- **Nonce验证**: 防止重放攻击（缓存30分钟）
- **签名验证**: 验证请求完整性和来源可信性

### 3. 错误处理
- **401**: 时间戳过期或签名验证失败
- **403**: 检测到重放攻击
- **400**: 缺少必需参数

## 签名算法步骤

### 客户端签名步骤

1. **准备请求参数**
   - 业务参数
   - appId（应用标识）
   - timestamp（当前时间戳，毫秒）
   - nonce（随机数）

2. **构造待签名字符串**
   - 按字典序排列所有参数（除sign外）
   - 格式：`key1=value1&key2=value2`

3. **计算签名**
   - 使用HMAC-SHA256算法
   - 密钥为应用密钥
   - 转换为十六进制字符串

4. **添加签名**
   - 将签名值作为sign参数添加到请求中

### 服务端验证步骤

1. **提取签名参数**
   - appId、timestamp、nonce、sign

2. **验证时间戳**
   - 检查是否在有效期内（默认5分钟）

3. **验证nonce**
   - 检查是否已被使用过（防重放攻击）

4. **验证签名**
   - 使用相同算法重新计算签名
   - 与客户端提供的签名进行比对

## 配置说明

### 配置文件
```yaml
bgai:
  signature:
    enabled: true  # 是否启用签名验证
    timestamp-expire-seconds: 300  # 时间戳有效期（秒）
    nonce-cache-expire-seconds: 1800  # nonce缓存过期时间（秒）
    app-secret-cache-expire-seconds: 3600  # 应用密钥缓存过期时间（秒）
```

### 排除路径
以下路径不需要签名验证：
- `/api/auth/` - 认证相关接口
- `/api/signature/` - 签名验证测试接口
- `/api/app-secret/` - 应用密钥管理接口
- `/docs`、`/swagger` - 文档接口
- `/health`、`/actuator` - 健康检查接口
- `/test-` - 测试接口

## 使用示例

### 1. 生成签名参数

```java
// 业务参数
Map<String, String> businessParams = new HashMap<>();
businessParams.put("param1", "value1");
businessParams.put("param2", "value2");

// 生成签名参数
Map<String, String> signatureParams = SignatureUtils.generateSignatureParams(
    "test-app-001", 
    "secret_test_app_001", 
    businessParams
);
```

### 2. 发送请求

```bash
curl -X GET "http://localhost:8688/api/signature/test" \
  -G \
  -d "appId=test-app-001" \
  -d "timestamp=1703123456789" \
  -d "nonce=abc123def456" \
  -d "sign=a1b2c3d4e5f6..." \
  -d "param1=value1" \
  -d "param2=value2"
```

### 3. 测试接口

#### 生成签名示例
```bash
GET /api/signature/generate-example?appId=test-app-001&secret=secret_test_app_001
```

#### 验证签名示例
```bash
POST /api/signature/verify-example
Content-Type: application/x-www-form-urlencoded

appId=test-app-001&timestamp=1703123456789&nonce=abc123def456&sign=a1b2c3d4e5f6...&param1=value1&param2=value2
```

#### 获取算法说明
```bash
GET /api/signature/algorithm-info
```

## 数据库表结构

### app_secret表
```sql
CREATE TABLE `app_secret` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `app_id` varchar(64) NOT NULL COMMENT '应用ID',
  `app_secret` varchar(128) NOT NULL COMMENT '应用密钥',
  `app_name` varchar(100) DEFAULT NULL COMMENT '应用名称',
  `description` varchar(500) DEFAULT NULL COMMENT '应用描述',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1-启用，0-禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除：1-已删除，0-未删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_id` (`app_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用密钥表';
```

## 管理接口

### 应用密钥管理

#### 查询应用密钥列表
```bash
GET /api/app-secret/list?current=1&size=10&appId=test&appName=测试
```

#### 创建应用密钥
```bash
POST /api/app-secret
Content-Type: application/json

{
  "appId": "new-app-001",
  "appSecret": "secret_new_app_001",
  "appName": "新应用001",
  "description": "新应用的描述"
}
```

#### 更新应用密钥
```bash
PUT /api/app-secret/{id}
Content-Type: application/json

{
  "appId": "updated-app-001",
  "appSecret": "secret_updated_app_001",
  "appName": "更新后的应用001",
  "description": "更新后的描述"
}
```

#### 删除应用密钥
```bash
DELETE /api/app-secret/{id}
```

#### 启用/禁用应用密钥
```bash
PUT /api/app-secret/{id}/status?status=1
```

## 安全建议

1. **密钥管理**
   - 定期轮换应用密钥
   - 使用强密码策略
   - 限制密钥访问权限

2. **时间戳设置**
   - 根据业务需求调整时间戳有效期
   - 考虑网络延迟和时钟偏差

3. **Nonce管理**
   - 合理设置nonce缓存时间
   - 监控nonce使用情况

4. **监控告警**
   - 监控签名验证失败率
   - 设置重放攻击告警
   - 记录异常请求日志

## 故障排查

### 常见错误

1. **401 - 时间戳过期**
   - 检查客户端时间是否正确
   - 调整时间戳有效期配置

2. **403 - 重放攻击**
   - 检查nonce是否重复使用
   - 确认nonce生成逻辑

3. **401 - 签名验证失败**
   - 检查签名算法实现
   - 确认密钥是否正确
   - 验证参数排序

### 调试方法

1. **启用调试日志**
   ```yaml
   logging:
     level:
       com.bgpay.bgai.filter: DEBUG
       com.bgpay.bgai.service: DEBUG
   ```

2. **使用测试接口**
   - `/api/signature/generate-example` - 生成测试参数
   - `/api/signature/verify-example` - 验证签名
   - `/api/signature/algorithm-info` - 查看算法说明

3. **检查Redis缓存**
   - 查看nonce缓存状态
   - 检查应用密钥缓存

## 扩展功能

### 支持JSON请求体签名
当前版本主要支持URL参数签名，可以扩展支持JSON请求体的签名验证。

### 支持多种签名算法
可以扩展支持其他签名算法，如RSA、ECDSA等。

### 支持签名版本管理
可以添加签名版本管理，支持不同版本的签名算法。

## 总结

接口签名验证功能通过时间戳+随机数+数字签名的三重保护，有效防止了接口被恶意调用、重放攻击和数据篡改。整个流程既保障了API的安全性，又通过合理的缓存策略平衡了性能与安全的需求。 