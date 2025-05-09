package com.bgpay.bgai.controller;

import com.bgpay.bgai.entity.UserToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的模拟认证控制器，用于本地开发测试
 * 替代外部SSO系统
 */
@RestController
@RequestMapping("/auth")
@Slf4j
public class MockAuthController {

    // 存储授权码和相关信息
    private final Map<String, Map<String, String>> authCodes = new ConcurrentHashMap<>();
    
    // 存储令牌和用户信息
    private final Map<String, UserToken> tokens = new ConcurrentHashMap<>();
    
    /**
     * 模拟授权端点
     */
    @GetMapping("/authorize")
    public ResponseEntity<Map<String, String>> authorize(
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("response_type") String responseType) {
        
        log.info("Authorization request: client_id={}, redirect_uri={}, response_type={}", 
                clientId, redirectUri, responseType);
        
        // 生成授权码
        String code = UUID.randomUUID().toString();
        
        // 存储授权码相关信息
        Map<String, String> codeInfo = new HashMap<>();
        codeInfo.put("client_id", clientId);
        codeInfo.put("redirect_uri", redirectUri);
        authCodes.put(code, codeInfo);
        
        // 构建重定向URL (实际使用时应该重定向，这里为了测试方便返回JSON)
        Map<String, String> response = new HashMap<>();
        response.put("code", code);
        response.put("redirect_url", redirectUri + "?code=" + code);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 模拟令牌端点
     */
    @PostMapping("/token")
    public ResponseEntity<?> token(
            @RequestParam(value = "grant_type", required = false) String grantType,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "client_secret", required = false) String clientSecret,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri) {
        
        log.info("Token request: grant_type={}, code={}, client_id={}", 
                grantType, code, clientId);
        
        // 简化验证逻辑，只要有请求就返回有效令牌
        // 在测试环境下，我们不做严格验证
        
        // 生成访问令牌和刷新令牌
        String accessToken = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString();
        
        // 创建用户令牌
        UserToken userToken = UserToken.builder()
//                .userId("mock-user-" + UUID.randomUUID().toString().substring(0, 8))
                .userId("689258T")
                .username("测试用户")
                .email("test@example.com")
                .accessToken(accessToken)
                .tokenExpireTime(LocalDateTime.now().plusHours(1))
                .loginTime(LocalDateTime.now())
                .valid(true)
                .build();
        
        // 存储令牌
        tokens.put(accessToken, userToken);
        
        // 如果有授权码，删除它
        if (code != null && !code.isEmpty()) {
            authCodes.remove(code);
        }
        
        // 返回令牌响应
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", accessToken);
        response.put("token_type", "Bearer");
        response.put("expires_in", 3600);
        response.put("refresh_token", refreshToken);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 模拟用户信息端点
     */
    @GetMapping("/userinfo")
    public ResponseEntity<?> userInfo(@RequestHeader("Authorization") String authorization) {
        String accessToken = authorization.replace("Bearer ", "");
        
        // 验证令牌
        if (!tokens.containsKey(accessToken)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "invalid_token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        
        UserToken userToken = tokens.get(accessToken);
        
        // 返回用户信息
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("user_id", userToken.getUserId());
        userInfo.put("name", userToken.getUsername());
        userInfo.put("email", userToken.getEmail());
        userInfo.put("picture", "https://ui-avatars.com/api/?name=" + userToken.getUsername());
        
        return ResponseEntity.ok(userInfo);
    }
    
    /**
     * 模拟刷新令牌端点
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestParam("refresh_token") String refreshToken) {
        // 生成新的访问令牌
        String newAccessToken = UUID.randomUUID().toString();
        String newRefreshToken = UUID.randomUUID().toString();
        
        // 创建令牌响应
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", newAccessToken);
        response.put("token_type", "Bearer");
        response.put("expires_in", 3600);
        response.put("refresh_token", newRefreshToken);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 模拟登出端点
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authorization) {
        String accessToken = authorization.replace("Bearer ", "");
        
        // 移除令牌
        tokens.remove(accessToken);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logout successful");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 验证令牌有效性端点
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authorization) {
        String accessToken = authorization.replace("Bearer ", "");
        
        // 验证令牌
        boolean isValid = tokens.containsKey(accessToken);
        UserToken userToken = tokens.get(accessToken);
        
        Map<String, Object> response = new HashMap<>();
        response.put("valid", isValid);
        
        if (isValid && userToken != null) {
            response.put("userId", userToken.getUserId());
            response.put("expiresAt", userToken.getTokenExpireTime());
        }
        
        return ResponseEntity.ok(response);
    }
} 