package com.bgpay.bgai.controller;

import com.bgpay.bgai.entity.User;
import com.bgpay.bgai.entity.UserToken;
import com.bgpay.bgai.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 身份认证控制器，处理SSO登录相关的请求
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private Environment environment;

    private String clientId;
    private String authorizeUrl;
    private String redirectUri;
    
    @PostConstruct
    public void init() {
        clientId = environment.getProperty("sso.client-id", "bgai-client-id");
        authorizeUrl = environment.getProperty("sso.authorize-url", "https://sso.bgpay.com/oauth2/authorize");
        redirectUri = environment.getProperty("sso.redirect-uri", "http://localhost:8080/api/auth/callback");
        
        log.info("初始化认证控制器: clientId={}, redirectUri={}", clientId, redirectUri);
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
            response.put("expiresAt", userToken.getTokenExpireTime());
            
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
            response.put("expiresAt", userToken.getTokenExpireTime());
            
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
                response.put("expiresAt", userToken.getTokenExpireTime());
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