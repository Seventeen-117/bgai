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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.annotation.PostConstruct;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 身份认证控制器，处理SSO登录相关的请求
 */
@Tag(name = "认证授权", description = "用户认证与授权相关接口")
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
            int configPort = Integer.parseInt(environment.getProperty("server.port", "8688"));
            this.serverPort = configPort;
            
            log.info("初始化认证控制器: clientId={}, 配置的端口={}", clientId, configPort);
            
            // 初始化URL，但实际端口可能会在服务器初始化事件中更新
            updateUrlConfigurations();
        } catch (Exception e) {
            log.warn("初始化认证控制器时出错: {}", e.getMessage());
            // 确保设置默认值
            if (clientId == null) clientId = "bgai-client-id";
            if (authorizeUrl == null) authorizeUrl = "https://sso.bgpay.com/oauth2/authorize";
            if (serverPort == 0) serverPort = 8688;
            updateUrlConfigurations();
        }
    }
    
    /**
     * 基于当前的serverPort更新所有URL配置
     */
    private void updateUrlConfigurations() {
        String hostname = environment.getProperty("sso.hostname", "localhost");
        String protocol = environment.getProperty("sso.protocol", "http");

        // 获取配置的redirectUri
        redirectUri = environment.getProperty("sso.redirect-uri");
        
        if (redirectUri == null || redirectUri.contains("${")) {
            // 如果没有配置或包含占位符，则使用实际端口构建
            redirectUri = protocol + "://" + hostname + ":" + serverPort + "/api/auth/callback";
            log.info("根据实际端口生成redirectUri: {}", redirectUri);
        } else if (redirectUri.contains("localhost:") && !redirectUri.contains(":" + serverPort)) {
            // 如果redirectUri中包含了端口，但不是当前实际端口，则进行替换
            String[] parts = redirectUri.split(":");
            if (parts.length >= 3) {
                String portPart = parts[2];
                int slashIndex = portPart.indexOf("/");
                if (slashIndex > 0) {
                    String oldPort = portPart.substring(0, slashIndex);
                    String newRedirectUri = redirectUri.replace(":" + oldPort, ":" + serverPort);
                    log.info("更新认证控制器redirectUri端口: {} → {}", redirectUri, newRedirectUri);
                    redirectUri = newRedirectUri;
                }
            }
        }
        
        log.info("认证控制器URL配置已更新: redirectUri={}, 系统实际运行端口={}", redirectUri, serverPort);
    }

    /**
     * 获取SSO登录URL
     * 
     * @return 登录URL
     */
    @Operation(
        summary = "获取SSO登录URL", 
        description = "生成用于SSO登录的完整URL，包含必要的参数",
        tags = {"认证授权"}
    )
    @ApiResponse(
        responseCode = "200", 
        description = "成功获取登录URL",
        content = @Content(mediaType = "application/json")
    )
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
    @Operation(
        summary = "处理SSO回调", 
        description = "使用授权码完成SSO登录流程，获取访问令牌和用户信息",
        tags = {"认证授权"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "登录成功",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "登录失败",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping("/callback")
    public ResponseEntity<?> handleCallback(
            @Parameter(description = "SSO授权码", required = true) 
            @RequestParam("code") String code
    ) {
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
    @Operation(
        summary = "刷新访问令牌", 
        description = "使用刷新令牌获取新的访问令牌",
        tags = {"认证授权"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "刷新成功",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "刷新失败",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @Parameter(description = "刷新令牌", required = true)
            @RequestParam("refresh_token") String refreshToken
    ) {
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
     * @param authorization 授权头
     * @return 退出结果
     */
    @Operation(
        summary = "退出登录", 
        description = "使用访问令牌退出当前登录会话",
        tags = {"认证授权"},
        security = {@SecurityRequirement(name = "BearerAuth")}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "退出成功",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "退出失败",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @Parameter(description = "认证令牌，格式：Bearer {token}", required = true)
            @RequestHeader("Authorization") String authorization
    ) {
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
    @Operation(
        summary = "获取当前用户信息", 
        description = "根据令牌获取当前登录用户的详细信息",
        tags = {"认证授权"},
        security = {@SecurityRequirement(name = "BearerAuth")}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取用户信息",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "认证失败",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo(
            @Parameter(description = "认证令牌，格式：Bearer {token}", required = true)
            @RequestHeader("Authorization") String authorization
    ) {
        try {
            // 提取令牌并验证
            String accessToken = authorization.replace("Bearer ", "");
            UserToken userToken = userService.validateToken(accessToken);
            
            if (userToken == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "无效的令牌");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            User user = userService.getUserInfo(userToken.getUserId());
            
            // 构建用户信息响应
            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getUserId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取用户信息时出错", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "获取用户信息失败");
            error.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * 验证令牌的有效性
     * 
     * @param authorization 授权头
     * @return 验证结果
     */
    @Operation(
        summary = "验证令牌有效性", 
        description = "校验访问令牌是否有效，并返回相关用户信息",
        tags = {"认证授权"},
        security = {@SecurityRequirement(name = "BearerAuth")}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "令牌有效",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "令牌无效",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(
            @Parameter(description = "认证令牌，格式：Bearer {token}", required = true)
            @RequestHeader("Authorization") String authorization
    ) {
        try {
            // 提取令牌并验证
            String accessToken = authorization.replace("Bearer ", "");
            UserToken userToken = userService.validateToken(accessToken);
            
            if (userToken == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "无效的令牌");
                error.put("valid", "false");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            // 构建验证响应
            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("userId", userToken.getUserId());
            response.put("username", userToken.getUsername());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("验证令牌时出错", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "验证令牌失败");
            error.put("message", e.getMessage());
            error.put("valid", "false");
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * 根据用户ID刷新令牌
     * 内部接口，需要管理员权限
     * 
     * @param userId 用户ID
     * @param authorization 授权头
     * @return 刷新结果，包含新的用户令牌
     */
    @Operation(
        summary = "根据用户ID刷新令牌",
        description = "系统内部接口，根据指定用户ID刷新其访问令牌，需要管理员权限",
        tags = {"认证授权"},
        security = {@SecurityRequirement(name = "BearerAuth")}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "刷新成功",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "未授权",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "权限不足",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "用户不存在",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping("/refresh-by-userid")
    public ResponseEntity<?> refreshTokenByUserId(
            @Parameter(description = "要刷新令牌的用户ID", required = true)
            @RequestParam("userId") String userId,
            @Parameter(description = "管理员认证令牌，格式：Bearer {token}", required = true)
            @RequestHeader("Authorization") String authorization) {
        try {
            // TODO: 验证调用者是否有管理员权限
            
            UserToken userToken = userService.refreshTokenByUserId(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userToken.getUserId());
            response.put("accessToken", userToken.getAccessToken());
            response.put("expiresAt", userToken.getTokenExpireTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("根据用户ID刷新令牌时出错: {}, {}", userId, e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "刷新令牌失败");
            error.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
} 