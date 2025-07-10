package com.bgpay.bgai.controller;

import com.bgpay.bgai.entity.ApiConfig;
import com.bgpay.bgai.entity.UsageInfo;
import com.bgpay.bgai.response.ChatResponse;
import com.bgpay.bgai.service.ApiConfigService;
import com.bgpay.bgai.service.deepseek.DeepSeekService;
import com.bgpay.bgai.transaction.TransactionCoordinator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * OpenAPI 兼容接口控制器，支持标准OpenAI API格式
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "OpenAPI兼容接口", description = "兼容OpenAI API格式的接口")
public class OpenApiController {

    private final DeepSeekService deepSeekService;
    private final ApiConfigService apiConfigService;
    private final TransactionCoordinator transactionCoordinator;
    
    /**
     * 处理聊天完成请求，兼容OpenAI API格式
     */
    @Operation(
        summary = "聊天完成请求", 
        description = "处理聊天完成请求，兼容OpenAI API格式"
    )
    @PostMapping(value = "/chat/completions", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ChatResponse>> chatCompletions(
            @RequestBody Map<String, Object> requestBody,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            ServerWebExchange exchange) {
            
        log.info("Received chat/completions request");
        
        // 从Authorization中提取API Key
        String apiKey = null;
        if (StringUtils.hasText(authorization)) {
            if (authorization.startsWith("Bearer ")) {
                apiKey = authorization.substring(7);
            } else {
                apiKey = authorization;
            }
        }
        
        // 获取模型名称
        String model = requestBody.containsKey("model") ? 
                String.valueOf(requestBody.get("model")) : "gpt-3.5-turbo";
        
        // 验证请求参数
        if (!requestBody.containsKey("messages")) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(createErrorResponse(400, "Messages are required")));
        }
        
        // 获取用户ID
        String userId = getUserIdFromExchange(exchange);
        
        // 查找匹配的API配置
        return findMatchingConfig(userId, null, apiKey, model)
            .flatMap(config -> {
                try {
                    // 调用深度学习服务
                    return deepSeekService.processRequestReactive(
                            requestBody, 
                            config.getApiUrl(), 
                            config.getApiKey(), 
                            config.getModelName(), 
                            userId, 
                            true // 默认支持多轮对话
                    )
                    .map(ResponseEntity::ok)
                    .onErrorResume(e -> {
                        log.error("处理chat/completions请求失败", e);
                        return Mono.just(ResponseEntity.status(500)
                                .body(createErrorResponse(500, e.getMessage())));
                    });
                } catch (Exception e) {
                    log.error("处理chat/completions请求发生异常", e);
                    return Mono.just(ResponseEntity.status(500)
                            .body(createErrorResponse(500, e.getMessage())));
                }
            })
            .onErrorResume(e -> {
                log.error("处理chat/completions请求失败", e);
                return Mono.just(ResponseEntity.status(500)
                        .body(createErrorResponse(500, e.getMessage())));
            });
    }
    
    /**
     * 创建错误响应
     */
    private ChatResponse createErrorResponse(int statusCode, String message) {
        ChatResponse errorResponse = new ChatResponse();
        errorResponse.setSuccess(false);
        errorResponse.setError(new ChatResponse.Error(statusCode, message));
        return errorResponse;
    }
    
    /**
     * 从请求上下文获取用户ID
     */
    private String getUserIdFromExchange(ServerWebExchange exchange) {
        // 首先尝试从头部获取
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (StringUtils.hasText(userId)) {
            return userId;
        }
        
        // 其次尝试从请求属性获取
        Object userIdAttr = exchange.getAttribute("userId");
        if (userIdAttr != null) {
            return userIdAttr.toString();
        }
        
        return null; // 如果未找到，返回null
    }
    
    /**
     * 查找匹配的API配置
     */
    private Mono<ApiConfig> findMatchingConfig(String userId, String apiUrl, String apiKey, String modelName) {
        return Mono.fromCallable(() -> {
            ApiConfig config = apiConfigService.findMatchingConfig(userId, apiUrl, apiKey, modelName);
            
            if (config != null) {
                // 如果传入了modelName，使用传入的modelName
                if (StringUtils.hasText(modelName)) {
                    config.setModelName(modelName);
                } else if (!StringUtils.hasText(config.getModelName())) {
                    // 如果配置中的modelName为空，使用默认值
                    config.setModelName("deepseek-chat");
                }
                return config;
            }
            
            // 如果没有找到配置，创建默认配置
            ApiConfig defaultConfig = new ApiConfig();
            defaultConfig.setApiUrl("https://api.deepseek.com");
            defaultConfig.setApiKey(apiKey != null ? apiKey : "anonymous");
            defaultConfig.setModelName(StringUtils.hasText(modelName) ? modelName : "deepseek-chat");
            defaultConfig.setUserId(userId != null ? userId : "anonymous");
            
            return defaultConfig;
        });
    }
} 