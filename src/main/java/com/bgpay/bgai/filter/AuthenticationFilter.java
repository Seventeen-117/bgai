package com.bgpay.bgai.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 认证过滤器
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2) // 在API Key过滤器之后执行
public class AuthenticationFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    
    @Value("${bgai.api-key.test-key:test-api-key-123}")
    private String testApiKey;

    // 不需要认证的路径
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/api/auth/", 
            "/api/simple-auth/",
            "/api/session/",
            "/docs", 
            "/swagger", 
            "/v3/api-docs", 
            "/health", 
            "/actuator",
            "/test-",
            "/api/feign/",
            "/api/mock",
            "/api/users",
            "/api/signature/", // 签名验证测试接口
            "/api/app-secret/", // 应用密钥管理接口
            "/favicon.ico");

    public AuthenticationFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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
        
        // 检查是否为内部Feign调用
        String apiKey = request.getHeader("X-API-Key");
        String requestFrom = request.getHeader("X-Request-From");
        if (testApiKey.equals(apiKey) && "bgtech-ai".equals(requestFrom)) {
            log.debug("允许内部Feign调用通过认证: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // 获取认证头
        String authHeader = request.getHeader("Authorization");

        // 认证头不存在
        if (authHeader == null || authHeader.isEmpty()) {
            log.debug("Missing authorization header for path: {}", path);
            handleUnauthorized(response);
            return;
        }

        // 简单验证Bearer token格式
        if (!authHeader.startsWith("Bearer ")) {
            log.debug("Invalid authorization header format for path: {}", path);
            handleUnauthorized(response);
            return;
        }

        // 这里只做简单格式验证，实际应用中应该验证token的有效性
        filterChain.doFilter(request, response);
    }

    /**
     * 检查是否为排除的路径
     */
    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith) || 
               path.contains("/public/");
    }

    /**
     * 处理未授权请求
     */
    private void handleUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(
                new java.util.HashMap<String, String>() {{
                    put("error", "Unauthorized");
                    put("message", "Missing authorization header");
                }}
        ));
    }
} 