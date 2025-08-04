package com.bgpay.bgai.filter;

import cn.hutool.core.util.StrUtil;
import com.bgpay.bgai.service.SignatureVerificationService;
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
 * WebFlux环境的接口签名验证过滤器
 * 实现HMAC-SHA256签名验证、时间戳验证和nonce防重放攻击
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 3) // 在API Key认证之后执行
public class SignatureVerificationWebFilter implements WebFilter {

    private final SignatureVerificationService signatureVerificationService;
    private final ObjectMapper objectMapper;

    @Value("${bgai.signature.enabled:true}")
    private boolean signatureEnabled;

    @Value("${bgai.signature.timestamp-expire-seconds:300}")
    private long timestampExpireSeconds;

    @Value("${bgai.signature.nonce-cache-expire-seconds:1800}")
    private long nonceCacheExpireSeconds;

    // 不需要签名验证的路径（与现有认证过滤器保持一致）
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/api/auth/", 
            "/auth/",
            "/docs", 
            "/swagger", 
            "/swagger-ui", 
            "/swagger-ui/", 
            "/swagger-resources", 
            "/swagger-resources/", 
            "/v3/api-docs", 
            "/v3/api-docs/", 
            "/webjars/", 
            "/health", 
            "/actuator",
            "/test-",
            "/api/session/",
            "/api/feign/local",
            "/api/mock",
            "/api/keys/generate",
            "/api/simple-auth/",
            "/api/feign/",
            "/api/users",
            "/favicon.ico");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 预检OPTIONS请求直接通过
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getPath().value();

        // 检查是否为排除的路径
        if (isExcludedPath(path)) {
            return chain.filter(exchange);
        }

        // 如果签名验证被禁用，直接通过
        if (!signatureEnabled) {
            return chain.filter(exchange);
        }

        return exchange.getFormData()
                .flatMap(formData -> {
                    // 提取签名参数
                    String appId = getParameter(exchange, "appId");
                    String timestamp = getParameter(exchange, "timestamp");
                    String nonce = getParameter(exchange, "nonce");
                    String sign = getParameter(exchange, "sign");

                    // 验证必需参数
                    if (StrUtil.isBlank(appId) || StrUtil.isBlank(timestamp) || 
                        StrUtil.isBlank(nonce) || StrUtil.isBlank(sign)) {
                        log.warn("Missing required signature parameters for path: {}", path);
                        return handleSignatureError(exchange, HttpStatus.BAD_REQUEST, "Missing required signature parameters");
                    }

                    // 验证时间戳
                    if (!signatureVerificationService.validateTimestamp(timestamp, timestampExpireSeconds)) {
                        log.warn("Timestamp expired for path: {}, timestamp: {}", path, timestamp);
                        return handleSignatureError(exchange, HttpStatus.UNAUTHORIZED, "Timestamp expired");
                    }

                    // 验证nonce防重放攻击
                    if (!signatureVerificationService.validateNonce(nonce, nonceCacheExpireSeconds)) {
                        log.warn("Nonce already used (replay attack detected) for path: {}, nonce: {}", path, nonce);
                        return handleSignatureError(exchange, HttpStatus.FORBIDDEN, "Replay attack detected");
                    }

                    // 获取所有请求参数
                    Map<String, String> allParams = getAllRequestParameters(exchange);
                    
                    // 验证签名
                    if (!signatureVerificationService.verifySignature(allParams, sign, appId)) {
                        log.warn("Signature verification failed for path: {}, appId: {}", path, appId);
                        return handleSignatureError(exchange, HttpStatus.UNAUTHORIZED, "Signature verification failed");
                    }

                    // 保存nonce到缓存
                    signatureVerificationService.saveNonce(nonce, nonceCacheExpireSeconds);

                    log.debug("Signature verification passed for path: {}, appId: {}", path, appId);
                    return chain.filter(exchange);
                })
                .onErrorResume(e -> {
                    log.error("Error during signature verification for path: {}", path, e);
                    return handleSignatureError(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Signature verification error");
                });
    }

    /**
     * 检查是否为排除的路径
     */
    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith) || 
               path.contains("/public/");
    }

    /**
     * 从请求中获取参数值
     */
    private String getParameter(ServerWebExchange exchange, String paramName) {
        List<String> values = exchange.getRequest().getQueryParams().get(paramName);
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    /**
     * 获取所有请求参数
     */
    private Map<String, String> getAllRequestParameters(ServerWebExchange exchange) {
        Map<String, String> params = new HashMap<>();
        
        // 获取URL参数
        exchange.getRequest().getQueryParams().forEach((key, values) -> {
            if (!values.isEmpty() && StrUtil.isNotBlank(values.get(0))) {
                params.put(key, values.get(0));
            }
        });

        // 获取表单参数（如果是POST/PUT请求）
        if (HttpMethod.POST.equals(exchange.getRequest().getMethod()) || 
            HttpMethod.PUT.equals(exchange.getRequest().getMethod())) {
            // 这里可以扩展支持JSON请求体的签名验证
            // 目前先支持URL参数
        }

        return params;
    }

    /**
     * 处理签名验证错误
     */
    private Mono<Void> handleSignatureError(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Signature verification failed");
        errorResponse.put("message", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        
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