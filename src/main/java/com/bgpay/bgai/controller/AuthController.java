package com.bgpay.bgai.controller;

import com.bgpay.bgai.entity.User;
import com.bgpay.bgai.entity.UserToken;
import com.bgpay.bgai.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 身份认证控制器，处理SSO登录相关的请求
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController implements ApplicationListener<WebServerInitializedEvent> {

    @Autowired
    private UserService userService;
    
    @Autowired
    private Environment environment;

    private String clientId;
    private String authorizeUrl;
    private String redirectUri;
    private int serverPort;
    private boolean serverInitialized = false;
    
    /**
     * 当应用服务器完全初始化后，会触发此事件
     * 用于获取实际运行的服务器端口
     * 注意：在测试环境中可能不会触发此事件
     */
    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        try {
            this.serverPort = event.getWebServer().getPort();
            this.serverInitialized = true;
            log.info("服务器已初始化，实际运行端口: {}", serverPort);
            
            // 更新URL配置
            updateUrlConfigurations();
        } catch (Exception e) {
            log.warn("处理服务器初始化事件时出错: {}", e.getMessage());
        }
    }
    
    @PostConstruct
    public void init() {
        try {
            // 从环境中加载SSO配置
            clientId = environment.getProperty("sso.client-id", "bgai-client-id");
            authorizeUrl = environment.getProperty("sso.authorize-url", "https://sso.bgpay.com/oauth2/authorize");
            
            // 暂时使用配置中的端口值，稍后在服务器初始化事件中更新为实际端口
            int configPort = Integer.parseInt(environment.getProperty("server.port", "8080"));
            this.serverPort = configPort;
            
            log.info("初始化认证控制器: clientId={}, 配置的端口={}", clientId, configPort);
            
            // 初始化URL，但实际端口可能会在服务器初始化事件中更新
            updateUrlConfigurations();
        } catch (Exception e) {
            log.warn("初始化认证控制器时出错: {}", e.getMessage());
            // 确保设置默认值
            if (clientId == null) clientId = "bgai-client-id";
            if (authorizeUrl == null) authorizeUrl = "https://sso.bgpay.com/oauth2/authorize";
            if (serverPort == 0) serverPort = 8080;
            updateUrlConfigurations();
        }
    }
    
    /**
     * 基于当前的serverPort更新所有URL配置
     */
    private void updateUrlConfigurations() {
        // 使用动态端口构建URL
        redirectUri = environment.getProperty("sso.redirect-uri", 
                "http://localhost:" + serverPort + "/api/auth/callback");
        
        log.info("更新认证控制器URL配置: redirectUri={}, 实际端口={}", redirectUri, serverPort);
    }

    /**
     * 获取SSO登录URL
     * 
     * @return 登录URL
     */
    @GetMapping("/login-url")
    public ResponseEntity<Map<String, String>> getLoginUrl() {
        String loginUrl = authorizeUrl +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=openid email profile";
        
        Map<String, String> response = new HashMap<>();
        response.put("loginUrl", loginUrl);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 处理SSO回调，完成登录流程
     * 
     * @param code SSO授权码
     * @return 登录结果，包含用户令牌
     */
    @PostMapping("/callback")
    public ResponseEntity<?> handleCallback(@RequestParam("code") String code) {
        try {
            UserToken userToken = userService.loginWithSSO(code);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userToken.getUserId());
            response.put("username", userToken.getUsername());
            response.put("accessToken", userToken.getAccessToken());
            response.put("expiresAt", userToken.getTokenExpireTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("SSO callback error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "登录失败");
            error.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * 刷新令牌
     * 
     * @param refreshToken 刷新令牌
     * @return 刷新结果，包含新的用户令牌
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestParam("refresh_token") String refreshToken) {
        try {
            UserToken userToken = userService.refreshToken(refreshToken);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userToken.getUserId());
            response.put("accessToken", userToken.getAccessToken());
            response.put("expiresAt", userToken.getTokenExpireTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token refresh error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "刷新令牌失败");
            error.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * 退出登录
     * 
     * @param accessToken 访问令牌
     * @return 退出结果
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String authorization) {
        try {
            String accessToken = authorization.replace("Bearer ", "");
            userService.logout(accessToken);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "退出成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Logout error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "退出失败");
            error.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 获取当前用户信息
     * 
     * @param authorization 授权头
     * @return 用户信息
     */
    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String authorization) {
        try {
            String accessToken = authorization.replace("Bearer ", "");
            UserToken userToken = userService.validateToken(accessToken);
            
            if (userToken == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "无效的令牌");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            User user = userService.getUserInfo(userToken.getUserId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getUserId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("avatarUrl", user.getAvatarUrl());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Get user info error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "获取用户信息失败");
            error.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 验证令牌有效性
     * 
     * @param authorization 授权头
     * @return 验证结果
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authorization) {
        try {
            String accessToken = authorization.replace("Bearer ", "");
            UserToken userToken = userService.validateToken(accessToken);
            
            Map<String, Object> response = new HashMap<>();
            if (userToken != null) {
                response.put("valid", true);
                response.put("userId", userToken.getUserId());
                response.put("expiresAt", userToken.getTokenExpireTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                return ResponseEntity.ok(response);
            } else {
                response.put("valid", false);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Token validation error", e);
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
} 