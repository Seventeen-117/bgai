package com.bgpay.bgai.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.bgpay.bgai.service.ApiKeyService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * API密钥认证过滤器
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // 在CORS过滤器之后执行
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${bgai.api-key.enabled:true}")
    private boolean apiKeyEnabled;

    @Value("${bgai.api-key.header-name:X-API-Key}")
    private String apiKeyHeader;
    
    @Value("${bgai.api-key.test-key:test-api-key-123}")
    private String testApiKey;

    // 不需要API Key的路径
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/api/auth/", 
            "/docs", 
            "/swagger", 
            "/v3/api-docs", 
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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 预检OPTIONS请求直接通过
        if (request.getMethod().equals(HttpMethod.OPTIONS.name())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // 检查是否为排除的路径
        if (isExcludedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 如果API Key校验被禁用，直接通过
        if (!apiKeyEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取API Key
        String apiKey = request.getHeader(apiKeyHeader);

        // API Key不存在
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("API Key is missing for path: {}", path);
            handleUnauthorized(response, "API Key is required");
            return;
        }
        
        // 如果是测试API Key且请求来源是内部服务，则通过验证
        if (testApiKey.equals(apiKey) && 
            ("bgtech-ai".equals(request.getHeader("X-Request-From")) || 
             request.getRequestURI().startsWith("/api/users"))) {
            log.debug("Test API key accepted for internal service call: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // 验证API Key
        try {
            ApiKeyService.ApiKeyValidationResult result = apiKeyService.validateApiKeyStatus(apiKey);
            
            // 检查是否需要严格验证API Key状态
            boolean isStrictValidationPath = isStrictValidationPath(path);
            
            if (result.status != ApiKeyService.ApiKeyStatus.VALID) {
                log.warn("Invalid API Key provided for path: {}, reason: {}, strict validation: {}", 
                        path, result.reason, isStrictValidationPath);
                handleUnauthorized(response, result.reason != null ? result.reason : "Invalid API Key");
                return;
            }
            
            // 对于严格验证路径，额外检查API Key的active状态
            if (isStrictValidationPath) {
                // 获取完整的API Key信息以检查active状态
                var keyInfo = apiKeyService.getApiKeyInfo(apiKey);
                if (keyInfo == null || keyInfo.getActive() == null || keyInfo.getActive() == 0) {
                    log.warn("Inactive API Key used for strict validation path: {}", path);
                    handleUnauthorized(response, "API Key is inactive");
                    return;
                }
                log.info("Strict API Key validation passed for path: {}", path);
            }
        } catch (Exception e) {
            log.error("Error validating API key: {}", e.getMessage());
            handleUnauthorized(response, "API Key validation error");
            return;
        }

        // API Key有效，继续处理请求
        filterChain.doFilter(request, response);
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
    private void handleUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(
                new java.util.HashMap<String, String>() {{
                    put("error", message);
                }}
        ));
    }
} 