package com.bgpay.bgai.filter;

import com.bgpay.bgai.entity.ApiKey;
import com.bgpay.bgai.service.ApiKeyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API密钥验证WebFilter，用于WebFlux环境下的API密钥验证
 * 特别针对chatGatWay-internal端点进行严格的API密钥状态验证
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 2) // 在CORS过滤器之后，但在其他业务过滤器之前执行
public class ApiKeyWebFilter implements WebFilter {

    private final ApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;

    @Value("${bgai.api-key.enabled:true}")
    private boolean apiKeyEnabled;

    @Value("${bgai.api-key.header-name:X-API-Key}")
    private String apiKeyHeader;
    
    @Value("${bgai.api-key.test-key:test-api-key-123}")
    private String testApiKey;

    // 不需要API Key的路径
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/api/auth/", 
            "/auth/", // 放行mock SSO/token服务
            "/docs", 
            "/swagger", 
            "/swagger-ui", // swagger UI
            "/swagger-ui/", // swagger UI
            "/swagger-resources", // swagger资源
            "/swagger-resources/", // swagger资源
            "/v3/api-docs", 
            "/v3/api-docs/", 
            "/webjars/", // swagger静态资源
            "/health", 
            "/actuator",
            "/test-",
            "/api/session/",
            "/api/feign/local",
            "/api/mock",
            "/api/keys/generate", // 添加API密钥生成接口到排除列表
            "/api/signature/", // 签名验证测试接口
            "/api/app-secret/", // 应用密钥管理接口
            "/favicon.ico");

    // 需要严格验证API Key状态的路径
    private static final List<String> STRICT_VALIDATION_PATHS = List.of(
            "/api/chatGatWay-internal"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 预检OPTIONS请求直接通过
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // 检查是否为排除的路径
        if (isExcludedPath(path)) {
            return chain.filter(exchange);
        }

        // 如果API Key校验被禁用，直接通过
        if (!apiKeyEnabled) {
            return chain.filter(exchange);
        }

        // 获取API Key
        List<String> apiKeyHeaders = exchange.getRequest().getHeaders().get(apiKeyHeader);
        String apiKey = apiKeyHeaders != null && !apiKeyHeaders.isEmpty() ? apiKeyHeaders.get(0) : null;

        // API Key不存在
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("API Key is missing for path: {}", path);
            return handleUnauthorized(exchange, "API Key is required");
        }
        
        // 如果是测试API Key且请求来源是内部服务，则通过验证
        List<String> requestFromHeaders = exchange.getRequest().getHeaders().get("X-Request-From");
        String requestFrom = requestFromHeaders != null && !requestFromHeaders.isEmpty() ? requestFromHeaders.get(0) : null;
        
        if (testApiKey.equals(apiKey) && 
            ("bgtech-ai".equals(requestFrom) || 
             path.startsWith("/api/users"))) {
            log.debug("Test API key accepted for internal service call: {}", path);
            return chain.filter(exchange);
        }

        // 检查是否需要严格验证API Key状态
        boolean isStrictValidationPath = isStrictValidationPath(path);

        // 验证API Key
        try {
            ApiKeyService.ApiKeyValidationResult result = apiKeyService.validateApiKeyStatus(apiKey);
            
            if (result.status != ApiKeyService.ApiKeyStatus.VALID) {
                log.warn("Invalid API Key provided for path: {}, reason: {}, strict validation: {}", 
                        path, result.reason, isStrictValidationPath);
                return handleUnauthorized(exchange, result.reason != null ? result.reason : "Invalid API Key");
            }
            
            // 对于严格验证路径，额外检查API Key的active状态
            if (isStrictValidationPath) {
                // 获取完整的API Key信息以检查active状态
                ApiKey keyInfo = apiKeyService.getApiKeyInfo(apiKey);
                if (keyInfo == null || keyInfo.getActive() == null || keyInfo.getActive() == 0) {
                    log.warn("Inactive API Key used for strict validation path: {}", path);
                    return handleUnauthorized(exchange, "API Key is inactive");
                }
                log.info("Strict API Key validation passed for path: {}", path);
            }
        } catch (Exception e) {
            log.error("Error validating API key: {}", e.getMessage());
            return handleUnauthorized(exchange, "API Key validation error");
        }

        // API Key有效，继续处理请求
        return chain.filter(exchange);
    }

    /**
     * 检查是否为排除的路径
     */
    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith) || 
               path.contains("/public/") || 
               HttpMethod.OPTIONS.name().equals(path);
    }
    
    /**
     * 检查是否为需要严格验证API Key状态的路径
     */
    private boolean isStrictValidationPath(String path) {
        return STRICT_VALIDATION_PATHS.stream().anyMatch(path::equals);
    }

    /**
     * 处理未授权请求
     */
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        
        try {
            byte[] responseBytes = objectMapper.writeValueAsBytes(errorResponse);
            return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(responseBytes))
            );
        } catch (Exception e) {
            log.error("Error writing error response", e);
            return exchange.getResponse().setComplete();
        }
    }
} 