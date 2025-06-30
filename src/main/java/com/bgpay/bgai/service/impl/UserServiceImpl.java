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
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
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
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import org.springframework.web.client.HttpClientErrorException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Set;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService, ApplicationListener<WebServerInitializedEvent> {

    private static final String TOKEN_KEY_PREFIX = "USER:TOKEN:";
    private static final String USER_INFO_KEY_PREFIX = "USER:INFO:";
    private static final int TOKEN_CACHE_DAYS = 7;
    private static final int USER_CACHE_DAYS = 30;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, UserToken> userTokenRedisTemplate;

    @Autowired(required = false)
    private RestTemplate restTemplate;
    
    @Autowired
    private Environment environment;
    
    // 将WebServerApplicationContext设为可选，解决测试环境中无法注入的问题
    @Autowired(required = false)
    private WebServerApplicationContext webServerAppCtx;
    
    // SSO配置属性
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String tokenUrl;
    private String userInfoUrl;
    private String logoutUrl;
    private int serverPort;
    private boolean serverInitialized = false;
    
    /**
     * 当应用服务器完全初始化后，会触发此事件
     * 用于获取实际运行的服务器端口
     */
    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        // 只有当服务器端口和当前端口不同时才更新
        int actualPort = event.getWebServer().getPort();
        if (this.serverPort != actualPort) {
            log.info("更新服务器端口: 从 {} 更新为 {}", this.serverPort, actualPort);
            this.serverPort = actualPort;
            // 更新URL配置
            updateUrlConfigurations();
        }
        this.serverInitialized = true;
        log.info("服务器已完全初始化，确认运行端口: {}", serverPort);
    }
    
    @PostConstruct
    public void init() {
        // 从环境中加载SSO配置
        clientId = environment.getProperty("sso.client-id", "bgai-client-id");
        clientSecret = environment.getProperty("sso.client-secret", "bgai-client-secret");
        
        // 通过ServerApplicationContext获取端口，而不是直接从配置中读取
        if (webServerAppCtx != null && webServerAppCtx.getWebServer() != null) {
            this.serverPort = webServerAppCtx.getWebServer().getPort();
            log.info("从WebServerApplicationContext获取实际端口: {}", this.serverPort);
        } else {
            // 仅在无法获取实际端口时使用配置中的端口作为备选
            this.serverPort = Integer.parseInt(environment.getProperty("server.port", "8688"));
            log.info("WebServerApplicationContext不可用或WebServer为null，使用配置端口: {}", this.serverPort);
        }
        
        log.info("初始化SSO配置: clientId={}, 使用端口={}", clientId, this.serverPort);
        
        // 初始化URL，但实际端口可能会在服务器初始化事件中更新
        updateUrlConfigurations();
        
        // 只在非测试环境中清理Redis数据，防止测试环境中出现问题
        if (webServerAppCtx != null && webServerAppCtx.getWebServer() != null) {
            cleanupOldRedisData();
        } else {
            log.info("测试环境中跳过Redis数据清理");
        }
    }
    
    /**
     * 基于当前的serverPort更新所有URL配置
     */
    private void updateUrlConfigurations() {
        String hostname = environment.getProperty("sso.hostname", "localhost");
        String protocol = environment.getProperty("sso.protocol", "http");

        // 使用动态端口构建URL，先检查是否在环境变量中已有完整配置
        redirectUri = environment.getProperty("sso.redirect-uri");
        if (redirectUri == null || redirectUri.contains("${")) {
            redirectUri = protocol + "://" + hostname + ":" + serverPort + "/api/auth/callback";
        }
        
        tokenUrl = environment.getProperty("sso.token-url");
        if (tokenUrl == null || tokenUrl.contains("${")) {
            tokenUrl = protocol + "://" + hostname + ":" + serverPort + "/auth/token";
        }
        
        userInfoUrl = environment.getProperty("sso.user-info-url");
        if (userInfoUrl == null || userInfoUrl.contains("${")) {
            userInfoUrl = protocol + "://" + hostname + ":" + serverPort + "/oauth2/userinfo";
        }
        
        logoutUrl = environment.getProperty("sso.logout-url");
        if (logoutUrl == null || logoutUrl.contains("${")) {
            logoutUrl = protocol + "://" + hostname + ":" + serverPort + "/oauth2/logout";
        }
        
        log.info("更新SSO URL配置: redirectUri={}, 实际端口={}", redirectUri, serverPort);
    }
    
    private void cleanupOldRedisData() {
        try {
            Set<String> tokenKeys = userTokenRedisTemplate.keys(TOKEN_KEY_PREFIX + "*");
            Set<String> userInfoKeys = userTokenRedisTemplate.keys(USER_INFO_KEY_PREFIX + "*");
            
            if (tokenKeys != null && !tokenKeys.isEmpty()) {
                userTokenRedisTemplate.delete(tokenKeys);
                log.info("已清理 {} 个旧的token缓存", tokenKeys.size());
            }
            
            if (userInfoKeys != null && !userInfoKeys.isEmpty()) {
                userTokenRedisTemplate.delete(userInfoKeys);
                log.info("已清理 {} 个旧的用户信息缓存", userInfoKeys.size());
            }
        } catch (Exception e) {
            log.error("清理旧的Redis数据时发生错误", e);
        }
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
            log.info("开始SSO登录流程, 授权码: {}", code);
            
            if (restTemplate == null) {
                log.warn("RestTemplate未注入，使用模拟用户数据");
                return createMockUserToken();
            }
            
            // 1. 获取 access_token
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("code", code);
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("redirect_uri", redirectUri);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded");
            headers.set("Accept", "application/json");
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

            log.debug("发送token请求: URL={}, params={}", tokenUrl, params);
            
            try {
                log.info("Token request URL: {}", tokenUrl);
                log.info("Token request params: {}", params);
                
                ResponseEntity<String> responseEntity = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    requestEntity,
                        String.class  // Change to String.class to see raw response
                );

                String rawResponse = responseEntity.getBody();
                log.info("Raw token response: {}", rawResponse);
                
                // Parse response manually to handle different formats
                Map<String, Object> tokenResponse;
                try {
                    if (rawResponse.startsWith("[")) {
                        // Response is a JSON array
                        if (rawResponse.contains("error")) {
                            // Error response in array format
                            if (rawResponse.contains("invalid_token")) {
                                throw new BillingException("授权码无效或已过期，请重新获取授权码");
                            }
                            throw new BillingException("获取访问令牌失败: " + rawResponse);
                        }
                        
                        // Try to extract the token from array format
                        String tokenValue = rawResponse.substring(rawResponse.indexOf("access_token") + "access_token".length() + 3);
                        tokenValue = tokenValue.substring(0, tokenValue.indexOf("\""));
                        
                        tokenResponse = new HashMap<>();
                        tokenResponse.put("access_token", tokenValue);
                        tokenResponse.put("token_type", "Bearer");
                        tokenResponse.put("expires_in", 3600);
                    } else if (rawResponse.startsWith("{")) {
                        // Response is a JSON object
                        ObjectMapper mapper = new ObjectMapper();
                        tokenResponse = mapper.readValue(rawResponse, Map.class);
                        
                        if (tokenResponse.containsKey("error")) {
                            String error = String.valueOf(tokenResponse.get("error"));
                            String errorDescription = tokenResponse.containsKey("error_description") ? 
                                String.valueOf(tokenResponse.get("error_description")) : "";
                            throw new BillingException(String.format("获取访问令牌失败: %s - %s", error, errorDescription));
                        }
                    } else {
                        // Response is possibly a direct token string
                        tokenResponse = new HashMap<>();
                        tokenResponse.put("access_token", rawResponse.trim());
                        tokenResponse.put("token_type", "Bearer");
                        tokenResponse.put("expires_in", 3600);
                    }
                } catch (Exception e) {
                    log.error("Failed to parse token response: {}", rawResponse, e);
                    throw new BillingException("解析访问令牌响应失败: " + e.getMessage());
                }

                if (!tokenResponse.containsKey("access_token")) {
                    log.error("Token response does not contain access_token: {}", tokenResponse);
                    throw new BillingException("获取访问令牌失败: 响应中缺少access_token");
                }

                String accessToken = String.valueOf(tokenResponse.get("access_token"));
                String refreshToken = tokenResponse.get("refresh_token") != null ? 
                    String.valueOf(tokenResponse.get("refresh_token")) : null;
                Integer expiresIn = tokenResponse.get("expires_in") instanceof Number ? 
                    ((Number) tokenResponse.get("expires_in")).intValue() : 3600;

            // 2. 获取用户信息
            HttpHeaders userInfoHeaders = new HttpHeaders();
            userInfoHeaders.set("Authorization", "Bearer " + accessToken);
                userInfoHeaders.set("Accept", "application/json");
            HttpEntity<Void> userInfoRequestEntity = new HttpEntity<>(userInfoHeaders);

                log.info("User info request URL: {}", userInfoUrl);
                log.debug("User info request headers: {}", userInfoHeaders);
                
                ResponseEntity<String> userInfoResponse = restTemplate.exchange(
                    userInfoUrl,
                    HttpMethod.GET,
                    userInfoRequestEntity,
                        String.class
                );

                String rawUserInfo = userInfoResponse.getBody();
                log.info("User info response status: {}", userInfoResponse.getStatusCode());
                log.debug("Raw user info response: {}", rawUserInfo);
                
                Map<String, Object> userInfo;
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
                mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
                
                try {
                    if (rawUserInfo == null || rawUserInfo.trim().isEmpty()) {
                        throw new BillingException("获取用户信息失败: 空响应");
                    }
                    
                    String trimmedResponse = rawUserInfo.trim();
                    if (trimmedResponse.startsWith("[")) {
                        // Response is a JSON array
                        JsonNode rootNode = mapper.readTree(trimmedResponse);
                        if (rootNode.size() == 0) {
                            throw new BillingException("获取用户信息失败: 空数组响应");
                        }
                        
                        // Get the first element if it's an array
                        JsonNode firstNode = rootNode.get(0);
                        if (firstNode.isObject()) {
                            userInfo = mapper.convertValue(firstNode, Map.class);
                        } else if (firstNode.isTextual()) {
                            // Handle case where array contains simple strings
                            String userId = firstNode.asText();
                            userInfo = new HashMap<>();
                            userInfo.put("user_id", userId);
                            userInfo.put("name", "User " + userId);
                            userInfo.put("email", userId + "@example.com");
                        } else {
                            log.error("Unexpected array element type: {}", firstNode.getNodeType());
                            throw new BillingException("获取用户信息失败: 数组元素格式错误");
                        }
                    } else if (trimmedResponse.startsWith("{")) {
                        // Response is a JSON object
                        userInfo = mapper.readValue(trimmedResponse, Map.class);
                    } else {
                        // Try to parse as JSON first
                        try {
                            JsonNode node = mapper.readTree(trimmedResponse);
                            if (node.isObject()) {
                                userInfo = mapper.convertValue(node, Map.class);
                            } else if (node.isTextual() || node.isNumber()) {
                                // Handle simple value as user ID
                                String userId = node.asText();
                                userInfo = new HashMap<>();
                                userInfo.put("user_id", userId);
                                userInfo.put("name", "User " + userId);
                                userInfo.put("email", userId + "@example.com");
                            } else {
                                throw new BillingException("获取用户信息失败: 未知的JSON格式");
                            }
                        } catch (JsonProcessingException e) {
                            // Not valid JSON, treat as plain text user ID
                            String userId = trimmedResponse;
                            userInfo = new HashMap<>();
                            userInfo.put("user_id", userId);
                            userInfo.put("name", "User " + userId);
                            userInfo.put("email", userId + "@example.com");
                        }
                    }

                    // Validate and normalize user info
                    if (!userInfo.containsKey("user_id") || userInfo.get("user_id") == null) {
                        log.error("User info missing required user_id field: {}", userInfo);
                        throw new BillingException("获取用户信息失败: 缺少用户ID");
                    }

                    // Normalize all fields to String type
                    String userId = String.valueOf(userInfo.get("user_id")).trim();
                    if (userId.isEmpty()) {
                        throw new BillingException("获取用户信息失败: 用户ID为空");
                    }

                    String username = userInfo.get("name") != null ? String.valueOf(userInfo.get("name")).trim() : "User " + userId;
                    String email = userInfo.get("email") != null ? String.valueOf(userInfo.get("email")).trim() : userId + "@example.com";
                    String avatarUrl = userInfo.get("picture") != null ? String.valueOf(userInfo.get("picture")).trim() : null;

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
                    userTokenRedisTemplate.opsForValue().set(tokenKey, userToken, TOKEN_CACHE_DAYS, TimeUnit.DAYS);

            // 缓存用户信息
            String userInfoKey = USER_INFO_KEY_PREFIX + userId;
                    userTokenRedisTemplate.opsForValue().set(userInfoKey, userToken, USER_CACHE_DAYS, TimeUnit.DAYS);

            return userToken;
                } catch (Exception e) {
                    log.error("Failed to get user info", e);
                    throw new BillingException("获取用户信息失败: " + e.getMessage());
                }
            } catch (HttpClientErrorException.Unauthorized ex) {
                log.error("Token endpoint returned unauthorized error", ex);
                String responseBody = ex.getResponseBodyAsString();
                if (responseBody != null && responseBody.contains("invalid_token")) {
                    throw new BillingException("授权码无效或已过期，请重新获取授权码");
                } else {
                    throw new BillingException("SSO认证失败: " + ex.getMessage());
                }
            } catch (HttpClientErrorException ex) {
                log.error("Token endpoint returned error", ex);
                throw new BillingException("SSO服务器错误: " + ex.getMessage());
            }
        } catch (BillingException e) {
            throw e;
        } catch (Exception e) {
            log.error("SSO登录异常", e);
            throw new RuntimeException("SSO登录失败: " + e.getMessage(), e);
        }
    }

    // 添加此帮助方法来创建模拟用户token
    private UserToken createMockUserToken() {
        UserToken token = new UserToken();
        token.setUserId("mock-user");
        token.setUsername("Mock User");
        token.setEmail("mock@example.com");
        token.setAccessToken("mock-token-" + System.currentTimeMillis());
        token.setTokenExpireTime(LocalDateTime.now().plusHours(1));
        token.setLoginTime(LocalDateTime.now());
        token.setValid(true);
        return token;
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
            log.warn("传入的accessToken为空");
            return null;
        }
        
        // 如果RestTemplate未注入，使用模拟数据
        if (restTemplate == null) {
            if (accessToken.startsWith("mock-token-")) {
                return createMockUserToken();
            }
            return null;
        }

        // 1. 首先检查缓存中是否存在令牌
        String tokenKey = TOKEN_KEY_PREFIX + accessToken;
        UserToken cachedToken = userTokenRedisTemplate.opsForValue().get(tokenKey);

        if (cachedToken != null) {
            // 检查令牌是否过期
            if (cachedToken.getTokenExpireTime().isAfter(LocalDateTime.now())) {
                return cachedToken;
            } else {
                // 令牌已过期，从缓存中删除
                userTokenRedisTemplate.delete(tokenKey);
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
                userTokenRedisTemplate.opsForValue().set(tokenKey, userToken, TOKEN_CACHE_DAYS, TimeUnit.DAYS);
                return userToken;
            }
        }

        // 3. 对于SimpleAuthController生成的测试令牌，仅开发环境允许
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isDev = java.util.Arrays.asList(activeProfiles).contains("dev");
        if (isDev && accessToken.length() == 36) { // UUID长度通常为36字符
            log.info("为测试令牌创建临时用户: {} (仅dev环境)", accessToken);
            UserToken testToken = UserToken.builder()
                    .userId("test-user-" + accessToken.substring(0, 8))
                    .username("测试用户")
                    .email("test@example.com")
                    .accessToken(accessToken)
                    .tokenExpireTime(LocalDateTime.now().plusDays(1))
                    .loginTime(LocalDateTime.now())
                    .valid(true)
                    .build();
            userTokenRedisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + accessToken, testToken, TOKEN_CACHE_DAYS, TimeUnit.DAYS);
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
        UserToken cachedUserToken = userTokenRedisTemplate.opsForValue().get(userInfoKey);

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

            userId = (String) userInfo.get("user_id");
            String username = (String) userInfo.get("name");
            String email = (String) userInfo.get("email");

            // 3. 更新用户信息
            User user = userMapper.findByUserId(userId);
            if (user == null) {
                throw new BillingException("用户不存在");
            }

            // 如果之前没有确定旧令牌，现在从数据库中获取
            if (oldAccessToken == null) {
                oldAccessToken = user.getAccessToken();
                log.info("从数据库获取用户[{}]的旧令牌: {}", userId, oldAccessToken);
            }
            
            user.setAccessToken(newAccessToken);
            user.setRefreshToken(newRefreshToken);
            user.setTokenExpireTime(tokenExpireTime);
            user.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(user);

            // 4. 更新缓存
            // 删除旧令牌
            if (oldAccessToken != null && !oldAccessToken.isEmpty()) {
                String oldTokenKey = TOKEN_KEY_PREFIX + oldAccessToken;
                log.info("删除旧令牌的Redis缓存: {}", oldTokenKey);
                userTokenRedisTemplate.delete(oldTokenKey);
            }

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
            userTokenRedisTemplate.opsForValue().set(newTokenKey, userToken, TOKEN_CACHE_DAYS, TimeUnit.DAYS);

            // 更新用户信息缓存
            String userInfoKey = USER_INFO_KEY_PREFIX + userId;
            userTokenRedisTemplate.opsForValue().set(userInfoKey, userToken, USER_CACHE_DAYS, TimeUnit.DAYS);
            
            log.info("成功刷新用户[{}]的令牌，旧令牌：{}，新令牌：{}", userId, oldAccessToken, newAccessToken);

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
            UserToken userToken = userTokenRedisTemplate.opsForValue().get(tokenKey);

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
                userTokenRedisTemplate.delete(tokenKey);

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

    /**
     * 根据用户ID刷新token
     * 
     * @param userId 用户ID
     * @return 新的用户令牌
     */
    @Override
    @Transactional
    @DS("master")
    public UserToken refreshTokenByUserId(String userId) {
        log.info("根据用户ID刷新token: {}", userId);
        
        // 1. 查询用户信息
        User user = userMapper.findByUserId(userId);
        if (user == null) {
            log.error("用户不存在: {}", userId);
            throw new BillingException("用户不存在");
        }
        
        // 保存旧令牌，确保稍后可以删除其缓存
        String oldAccessToken = user.getAccessToken();
        log.info("用户[{}]的旧令牌: {}", userId, oldAccessToken);
        
        // 2. 获取刷新令牌
        String refreshToken = user.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.error("用户没有可用的刷新令牌: {}", userId);
            throw new BillingException("用户没有可用的刷新令牌");
        }
        
        try {
            // 3. 调用刷新令牌的方法
            UserToken newToken = refreshToken(refreshToken);
            
            // 4. 确保删除了旧令牌的所有缓存
            if (oldAccessToken != null && !oldAccessToken.isEmpty()) {
                String oldTokenKey = TOKEN_KEY_PREFIX + oldAccessToken;
                log.info("显式删除旧令牌的Redis缓存: {}", oldTokenKey);
                userTokenRedisTemplate.delete(oldTokenKey);
            }
            
            return newToken;
        } catch (Exception e) {
            log.error("刷新令牌失败: {}", e.getMessage(), e);
            
            // 5. 如果刷新失败，尝试直接生成新的令牌（仅在开发环境）
            String[] activeProfiles = environment.getActiveProfiles();
            boolean isDev = java.util.Arrays.asList(activeProfiles).contains("dev");
            
            if (isDev) {
                log.warn("在开发环境中，尝试直接生成新的令牌: {}", userId);
                
                // 5.1 删除旧令牌的缓存
                if (oldAccessToken != null && !oldAccessToken.isEmpty()) {
                    String oldTokenKey = TOKEN_KEY_PREFIX + oldAccessToken;
                    log.info("显式删除旧令牌的Redis缓存(失败后): {}", oldTokenKey);
                    userTokenRedisTemplate.delete(oldTokenKey);
                }
                
                // 生成新的令牌
                String newAccessToken = java.util.UUID.randomUUID().toString();
                LocalDateTime tokenExpireTime = LocalDateTime.now().plusDays(1);
                
                // 更新用户信息
                user.setAccessToken(newAccessToken);
                user.setTokenExpireTime(tokenExpireTime);
                user.setUpdateTime(LocalDateTime.now());
                userMapper.updateById(user);
                
                // 创建用户令牌
                UserToken userToken = UserToken.builder()
                        .userId(userId)
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .accessToken(newAccessToken)
                        .tokenExpireTime(tokenExpireTime)
                        .loginTime(LocalDateTime.now())
                        .valid(true)
                        .build();
                
                // 缓存令牌
                String tokenKey = TOKEN_KEY_PREFIX + newAccessToken;
                userTokenRedisTemplate.opsForValue().set(tokenKey, userToken, TOKEN_CACHE_DAYS, TimeUnit.DAYS);
                
                // 更新用户信息缓存
                String userInfoKey = USER_INFO_KEY_PREFIX + userId;
                userTokenRedisTemplate.opsForValue().set(userInfoKey, userToken, USER_CACHE_DAYS, TimeUnit.DAYS);
                
                return userToken;
            }
            
            // 非开发环境抛出异常
            throw new BillingException("刷新令牌失败: " + e.getMessage());
        }
    }
} 