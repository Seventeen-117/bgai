package com.bgpay.bgai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bgpay.bgai.datasource.DS;
import com.bgpay.bgai.entity.User;
import com.bgpay.bgai.entity.UserToken;
import com.bgpay.bgai.exception.BillingException;
import com.bgpay.bgai.mapper.UserMapper;
import com.bgpay.bgai.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String TOKEN_KEY_PREFIX = "USER:TOKEN:";
    private static final String USER_INFO_KEY_PREFIX = "USER:INFO:";
    private static final int TOKEN_CACHE_DAYS = 7;
    private static final int USER_CACHE_DAYS = 30;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private Environment environment;
    
    // SSO配置属性
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String tokenUrl;
    private String userInfoUrl;
    private String logoutUrl;
    
    @PostConstruct
    public void init() {
        // 从环境中加载SSO配置
        clientId = environment.getProperty("sso.client-id", "bgai-client-id");
        clientSecret = environment.getProperty("sso.client-secret", "bgai-client-secret");
        redirectUri = environment.getProperty("sso.redirect-uri", "http://localhost:8080/api/auth/callback");
        tokenUrl = environment.getProperty("sso.token-url", "http://localhost:8080/auth/token");
        userInfoUrl = environment.getProperty("sso.user-info-url", "https://localhost:8080/oauth2/userinfo");
        logoutUrl = environment.getProperty("sso.logout-url", "https://localhost:8080/oauth2/logout");
        
        log.info("初始化SSO配置: clientId={}, redirectUri={}", clientId, redirectUri);
    }

    /**
     * 通过SSO授权码登录
     *
     * @param code SSO授权码
     * @return 用户令牌
     */
    @Override
    @Transactional
    @DS("master")
    public UserToken loginWithSSO(String code) {
        try {
            // 1. 获取 access_token
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("code", code);
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("redirect_uri", redirectUri);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded");
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            Map<String, Object> tokenResponse = responseEntity.getBody();
            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                log.error("Failed to get access token: {}", tokenResponse);
                throw new BillingException("获取访问令牌失败");
            }

            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");
            Integer expiresIn = (Integer) tokenResponse.get("expires_in");

            // 2. 获取用户信息
            HttpHeaders userInfoHeaders = new HttpHeaders();
            userInfoHeaders.set("Authorization", "Bearer " + accessToken);
            HttpEntity<Void> userInfoRequestEntity = new HttpEntity<>(userInfoHeaders);

            ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
                    userInfoUrl,
                    HttpMethod.GET,
                    userInfoRequestEntity,
                    Map.class
            );

            Map<String, Object> userInfo = userInfoResponse.getBody();
            if (userInfo == null || !userInfo.containsKey("user_id")) {
                log.error("Failed to get user info: {}", userInfo);
                throw new BillingException("获取用户信息失败");
            }

            String userId = (String) userInfo.get("user_id");
            String username = (String) userInfo.get("name");
            String email = (String) userInfo.get("email");
            String avatarUrl = (String) userInfo.get("picture");

            // 3. 更新或创建用户
            User existingUser = userMapper.findByUserId(userId);
            LocalDateTime tokenExpireTime = LocalDateTime.now().plus(expiresIn, ChronoUnit.SECONDS);

            if (existingUser == null) {
                // 新用户，创建记录
                User newUser = User.builder()
                        .userId(userId)
                        .username(username)
                        .email(email)
                        .avatarUrl(avatarUrl)
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .tokenExpireTime(tokenExpireTime)
                        .lastLoginTime(LocalDateTime.now())
                        .status(1)
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .build();
                userMapper.insert(newUser);
            } else {
                // 更新现有用户
                existingUser.setUsername(username);
                existingUser.setEmail(email);
                existingUser.setAvatarUrl(avatarUrl);
                existingUser.setAccessToken(accessToken);
                existingUser.setRefreshToken(refreshToken);
                existingUser.setTokenExpireTime(tokenExpireTime);
                existingUser.setLastLoginTime(LocalDateTime.now());
                existingUser.setUpdateTime(LocalDateTime.now());
                userMapper.updateById(existingUser);
            }

            // 4. 创建并缓存用户令牌
            UserToken userToken = UserToken.builder()
                    .userId(userId)
                    .username(username)
                    .email(email)
                    .accessToken(accessToken)
                    .tokenExpireTime(tokenExpireTime)
                    .loginTime(LocalDateTime.now())
                    .valid(true)
                    .build();

            // 缓存令牌
            String tokenKey = TOKEN_KEY_PREFIX + accessToken;
            redisTemplate.opsForValue().set(tokenKey, userToken, TOKEN_CACHE_DAYS, TimeUnit.DAYS);

            // 缓存用户信息
            String userInfoKey = USER_INFO_KEY_PREFIX + userId;
            redisTemplate.opsForValue().set(userInfoKey, userToken, USER_CACHE_DAYS, TimeUnit.DAYS);

            return userToken;
        } catch (Exception e) {
            log.error("SSO login failed", e);
            throw new BillingException("SSO登录失败: " + e.getMessage());
        }
    }

    /**
     * 验证令牌有效性
     *
     * @param accessToken 访问令牌
     * @return 用户令牌，如果无效返回null
     */
    @Override
    public UserToken validateToken(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return null;
        }

        // 1. 首先检查缓存中是否存在令牌
        String tokenKey = TOKEN_KEY_PREFIX + accessToken;
        UserToken cachedToken = (UserToken) redisTemplate.opsForValue().get(tokenKey);

        if (cachedToken != null) {
            // 检查令牌是否过期
            if (cachedToken.getTokenExpireTime().isAfter(LocalDateTime.now())) {
                return cachedToken;
            } else {
                // 令牌已过期，从缓存中删除
                redisTemplate.delete(tokenKey);
                return null;
            }
        }

        // 2. 缓存中不存在，从数据库中查询
        User user = userMapper.findByAccessToken(accessToken);
        if (user != null) {
            // 检查令牌是否过期
            if (user.getTokenExpireTime().isAfter(LocalDateTime.now())) {
                // 创建令牌对象
                UserToken userToken = UserToken.builder()
                        .userId(user.getUserId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .accessToken(accessToken)
                        .tokenExpireTime(user.getTokenExpireTime())
                        .loginTime(user.getLastLoginTime())
                        .valid(true)
                        .build();

                // 缓存令牌
                redisTemplate.opsForValue().set(tokenKey, userToken, TOKEN_CACHE_DAYS, TimeUnit.DAYS);
                return userToken;
            }
        }

        // 3. 对于SimpleAuthController生成的测试令牌，直接返回一个临时令牌
        // 此代码仅用于开发环境，生产环境应该移除
        if (accessToken.length() == 36) { // UUID长度通常为36字符
            log.info("为测试令牌创建临时用户: {}", accessToken);
            UserToken testToken = UserToken.builder()
                    .userId("test-user-" + accessToken.substring(0, 8))
                    .username("测试用户")
                    .email("test@example.com")
                    .accessToken(accessToken)
                    .tokenExpireTime(LocalDateTime.now().plusDays(1))
                    .loginTime(LocalDateTime.now())
                    .valid(true)
                    .build();
            
            // 缓存测试令牌
            redisTemplate.opsForValue().set(tokenKey, testToken, TOKEN_CACHE_DAYS, TimeUnit.DAYS);
            return testToken;
        }

        return null;
    }

    /**
     * 获取用户信息
     *
     * @param userId 用户ID
     * @return 用户实体
     */
    @Override
    @DS("slave")
    public User getUserInfo(String userId) {
        // 1. 首先检查缓存中是否存在用户信息
        String userInfoKey = USER_INFO_KEY_PREFIX + userId;
        UserToken cachedUserToken = (UserToken) redisTemplate.opsForValue().get(userInfoKey);

        if (cachedUserToken != null) {
            // 从缓存的用户令牌构建用户信息
            return User.builder()
                    .userId(cachedUserToken.getUserId())
                    .username(cachedUserToken.getUsername())
                    .email(cachedUserToken.getEmail())
                    .build();
        }

        // 2. 从数据库中查询用户信息
        return userMapper.findByUserId(userId);
    }

    /**
     * 刷新令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新的用户令牌
     */
    @Override
    @Transactional
    @DS("master")
    public UserToken refreshToken(String refreshToken) {
        try {
            // 1. 发送刷新令牌请求
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("refresh_token", refreshToken);
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded");
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            Map<String, Object> tokenResponse = responseEntity.getBody();
            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                log.error("Failed to refresh token: {}", tokenResponse);
                throw new BillingException("刷新令牌失败");
            }

            String newAccessToken = (String) tokenResponse.get("access_token");
            String newRefreshToken = (String) tokenResponse.get("refresh_token");
            Integer expiresIn = (Integer) tokenResponse.get("expires_in");
            LocalDateTime tokenExpireTime = LocalDateTime.now().plus(expiresIn, ChronoUnit.SECONDS);

            // 2. 获取用户信息
            HttpHeaders userInfoHeaders = new HttpHeaders();
            userInfoHeaders.set("Authorization", "Bearer " + newAccessToken);
            HttpEntity<Void> userInfoRequestEntity = new HttpEntity<>(userInfoHeaders);

            ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
                    userInfoUrl,
                    HttpMethod.GET,
                    userInfoRequestEntity,
                    Map.class
            );

            Map<String, Object> userInfo = userInfoResponse.getBody();
            if (userInfo == null || !userInfo.containsKey("user_id")) {
                log.error("Failed to get user info: {}", userInfo);
                throw new BillingException("获取用户信息失败");
            }

            String userId = (String) userInfo.get("user_id");
            String username = (String) userInfo.get("name");
            String email = (String) userInfo.get("email");

            // 3. 更新用户信息
            User user = userMapper.findByUserId(userId);
            if (user == null) {
                throw new BillingException("用户不存在");
            }

            user.setAccessToken(newAccessToken);
            user.setRefreshToken(newRefreshToken);
            user.setTokenExpireTime(tokenExpireTime);
            user.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(user);

            // 4. 更新缓存
            // 删除旧令牌
            String oldTokenKey = TOKEN_KEY_PREFIX + user.getAccessToken();
            redisTemplate.delete(oldTokenKey);

            // 创建新的用户令牌
            UserToken userToken = UserToken.builder()
                    .userId(userId)
                    .username(username)
                    .email(email)
                    .accessToken(newAccessToken)
                    .tokenExpireTime(tokenExpireTime)
                    .loginTime(LocalDateTime.now())
                    .valid(true)
                    .build();

            // 缓存新令牌
            String newTokenKey = TOKEN_KEY_PREFIX + newAccessToken;
            redisTemplate.opsForValue().set(newTokenKey, userToken, TOKEN_CACHE_DAYS, TimeUnit.DAYS);

            // 更新用户信息缓存
            String userInfoKey = USER_INFO_KEY_PREFIX + userId;
            redisTemplate.opsForValue().set(userInfoKey, userToken, USER_CACHE_DAYS, TimeUnit.DAYS);

            return userToken;
        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            throw new BillingException("刷新令牌失败: " + e.getMessage());
        }
    }

    /**
     * 登出
     *
     * @param accessToken 访问令牌
     */
    @Override
    public void logout(String accessToken) {
        try {
            // 1. 从缓存中获取用户令牌
            String tokenKey = TOKEN_KEY_PREFIX + accessToken;
            UserToken userToken = (UserToken) redisTemplate.opsForValue().get(tokenKey);

            if (userToken != null) {
                // 2. 调用SSO登出接口
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + accessToken);
                HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

                restTemplate.exchange(
                        logoutUrl,
                        HttpMethod.POST,
                        requestEntity,
                        Void.class
                );

                // 3. 删除令牌缓存
                redisTemplate.delete(tokenKey);

                // 4. 在数据库中将令牌置为无效
                User user = userMapper.findByUserId(userToken.getUserId());
                if (user != null && accessToken.equals(user.getAccessToken())) {
                    user.setAccessToken("");
                    user.setUpdateTime(LocalDateTime.now());
                    userMapper.updateById(user);
                }
            }
        } catch (Exception e) {
            log.error("Failed to logout", e);
            // 登出异常不需要抛出，只需记录日志
        }
    }
} 