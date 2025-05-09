package com.bgpay.bgai.filter;

import com.bgpay.bgai.config.ApiKeyConfig;
import com.bgpay.bgai.exception.ApiKeyAuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class ApiKeyAuthenticationFilter implements WebFilter {

    private final ApiKeyConfig apiKeyConfig;
    private final List<String> excludedPaths;

    public ApiKeyAuthenticationFilter(ApiKeyConfig apiKeyConfig) {
        this.apiKeyConfig = apiKeyConfig;
        // 配置不需要API Key验证的路径
        this.excludedPaths = List.of(
            // 认证相关接口 - 所有认证相关的路径
            "/api/auth/callback",    // 认证回调接口
            "/api/auth/login-url",   // 登录URL接口
            "/api/auth/refresh",     // 刷新令牌接口
            "/auth/",               // 基础认证路径下的所有接口
            "/api/auth/",           // API认证路径下的所有接口
            "/api/simple-auth/",    // 简单认证路径下的所有接口
            // API Key管理接口 - 所有API Key相关的路径
            "/api/keys/",           // API Key基础路径下的所有接口
            // Swagger UI和API文档
            "/swagger-ui/",
            "/v3/api-docs",
            // 监控端点
            "/actuator/",
            // 健康检查
            "/health",
            // 其他不需要验证的路径
            "/error"
        );
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // 检查是否是排除的路径
        if (isExcludedPath(path)) {
            log.debug("Skipping API Key validation for excluded path: {}", path);
            return chain.filter(exchange);
        }

        String apiKey = exchange.getRequest().getHeaders().getFirst(apiKeyConfig.getHeaderName());
        
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("API Key is missing for path: {}", path);
            return handleError(exchange, "API Key is required");
        }

        if (!isValidApiKey(apiKey)) {
            log.warn("Invalid API Key provided for path: {}", path);
            return handleError(exchange, "Invalid API Key");
        }

        // 将clientId添加到请求属性中，以便后续使用
        String clientId = apiKeyConfig.getApiKeys().get(apiKey);
        exchange.getAttributes().put("clientId", clientId);
        
        return chain.filter(exchange);
    }

    private boolean isExcludedPath(String path) {
        return excludedPaths.stream().anyMatch(excludedPath -> 
            path.equals(excludedPath) || path.startsWith(excludedPath));
    }

    private boolean isValidApiKey(String apiKey) {
        return apiKeyConfig.getApiKeys() != null && 
               apiKeyConfig.getApiKeys().containsKey(apiKey);
    }

    private Mono<Void> handleError(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        
        String responseBody = String.format("{\"error\":\"%s\"}", message);
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse()
                        .bufferFactory()
                        .wrap(responseBody.getBytes())));
    }
} 