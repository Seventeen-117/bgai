package com.bgpay.bgai.filter;

import com.bgpay.bgai.config.ApiKeyConfig;
import com.bgpay.bgai.entity.ApiKey;
import com.bgpay.bgai.exception.ApiKeyAuthenticationException;
import com.bgpay.bgai.service.ApiKeyService;
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

    private final ApiKeyService apiKeyService;
    private final List<String> excludedPaths;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
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

        String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
        
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("API Key is missing for path: {}", path);
            return handleError(exchange, "API Key is required");
        }

        ApiKeyService.ApiKeyValidationResult result = apiKeyService.validateApiKeyStatus(apiKey);
        if (result.status == ApiKeyService.ApiKeyStatus.VALID) {
            if (result.clientId != null) {
                exchange.getAttributes().put("clientId", result.clientId);
            }
            return chain.filter(exchange);
        } else {
            log.warn("API Key check failed for path: {}, reason: {}", path, result.reason);
            // 返回详细结构体
            return exchange.getResponse().writeWith(
                reactor.core.publisher.Mono.just(
                    exchange.getResponse().bufferFactory().wrap(
                        ("{" +
                            "\"status\":\"" + result.status + "\"," +
                            (result.expiresAt != null ? "\"expiresAt\":\"" + result.expiresAt + "\"," : "") +
                            (result.reason != null ? "\"reason\":\"" + result.reason + "\"" : "") +
                        "}").getBytes(java.nio.charset.StandardCharsets.UTF_8)
                    )
                )
            );
        }
    }

    private boolean isExcludedPath(String path) {
        return excludedPaths.stream().anyMatch(excludedPath -> 
            path.equals(excludedPath) || path.startsWith(excludedPath));
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