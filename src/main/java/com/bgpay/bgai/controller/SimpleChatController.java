package com.bgpay.bgai.controller;

import com.bgpay.bgai.response.SimpleChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpStatus;
import com.bgpay.bgai.entity.User;
import com.bgpay.bgai.mapper.UserMapper;

/**
 * 简化版的聊天控制器，提供更精简的API响应
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class SimpleChatController {

    private final ReactiveChatController reactiveChatController;
    @Value("${spring.profiles.active}")
    private String[] activeProfiles;
    private final Environment environment;
    private final UserMapper userMapper;
    
    @Autowired
    public SimpleChatController(ReactiveChatController reactiveChatController, Environment environment, UserMapper userMapper) {
        this.reactiveChatController = reactiveChatController;
        this.environment = environment;
        this.userMapper = userMapper;
    }

    /**
     * 处理聊天请求，返回简化版响应
     * 使用与原接口相同的路径，但返回更简洁的响应格式
     */
    @PostMapping(
            value = "/chatGatWay",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<SimpleChatResponse>> handleSimpleChatRequest(
            @RequestPart(value = "file", required = false) FilePart file,
            @RequestPart(value = "question", required =  false) String question,
            @RequestPart(value = "apiUrl", required = false) String apiUrl,
            @RequestPart(value = "apiKey", required = false) String apiKey,
            @RequestPart(value = "modelName", required = false) String modelName,
            @RequestPart(value = "multiTurn", required =  false) boolean multiTurn,
            ServerWebExchange exchange) {
        
        log.info("Received chatGatWay request: question length = {}, file = {}, modelName = {}", 
                question.length(), file != null ? file.filename() : "none", modelName);
        
        // 记录模型参数传递过程，用于调试
        exchange.getAttributes().put("requestedModelName", modelName);
        
        // 复用ReactiveChatController的handleChatRequest方法处理请求
        return reactiveChatController.handleChatRequest(
                    file, question, apiUrl, apiKey, modelName, String.valueOf(multiTurn), exchange
                )
                .map(ResponseEntity::getBody)
                .map(response -> {
                    // 记录最终使用的模型名称
                    if (response != null && response.getUsage() != null) {
                        String actualModelName = response.getUsage().getModelType();
                        log.info("Model name transformation: requested='{}', actual='{}'", 
                                modelName, actualModelName);
                    }
                    return response;
                })
                .map(SimpleChatResponse::fromChatResponse)
                .map(response -> {
                    // 如果有错误，设置对应的HTTP状态码
                    if (response.getError() != null) {
                        return ResponseEntity.status(response.getError().getCode()).body(response);
                    }
                    return ResponseEntity.ok(response);
                })
                .doOnNext(resp -> log.info("chatGatWay response prepared: {}", 
                        resp.getStatusCode()));
    }

    /**
     * 开发环境获取管理员token接口
     * 仅在开发环境生效，用于测试
     * 
     * @return 管理员token信息
     */
    @GetMapping("/dev-admin-token")
    public ResponseEntity<?> getDevAdminToken() {
        // 检查是否是开发环境
        boolean isDev = java.util.Arrays.asList(activeProfiles).contains("dev");
        
        if (!isDev) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "此接口仅在开发环境可用");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }
        
        try {
            // 查询管理员信息
            User adminUser = userMapper.findByUserId("admin-system");
            
            if (adminUser == null) {
                // 如果不存在管理员账户，则自动创建一个临时管理员
                String adminId = "admin-dev";
                String adminToken = UUID.randomUUID().toString();
                LocalDateTime expireTime = LocalDateTime.now().plusDays(7);
                
                User newAdmin = new User();
                newAdmin.setUserId(adminId);
                newAdmin.setUsername("开发管理员");
                newAdmin.setEmail("dev-admin@example.com");
                newAdmin.setAccessToken(adminToken);
                newAdmin.setRefreshToken(UUID.randomUUID().toString());
                newAdmin.setTokenExpireTime(expireTime);
                newAdmin.setCreateTime(LocalDateTime.now());
                newAdmin.setUpdateTime(LocalDateTime.now());
                newAdmin.setLastLoginTime(LocalDateTime.now());
                userMapper.insert(newAdmin);
                
                // 返回新创建的管理员token
                Map<String, Object> response = new HashMap<>();
                response.put("userId", adminId);
                response.put("accessToken", adminToken);
                response.put("expiresAt", expireTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                response.put("note", "已自动创建开发环境管理员账户");
                
                return ResponseEntity.ok(response);
            } else {
                // 如果已存在管理员账户，返回其token信息
                Map<String, Object> response = new HashMap<>();
                response.put("userId", adminUser.getUserId());
                response.put("accessToken", adminUser.getAccessToken());
                response.put("expiresAt", adminUser.getTokenExpireTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("获取开发管理员token失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "获取开发管理员token失败");
            error.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
} 