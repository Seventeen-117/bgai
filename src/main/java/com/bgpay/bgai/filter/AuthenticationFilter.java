package com.bgpay.bgai.filter;

import com.bgpay.bgai.entity.UserToken;
import com.bgpay.bgai.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证过滤器，验证请求的令牌有效性
 */
@Component
@Slf4j
public class AuthenticationFilter implements WebFilter {

    @Autowired
    private UserService userService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    // 不需要认证的路径
    private final List<String> whiteList = Arrays.asList(
            "/api/auth/login-url",
            "/api/auth/callback",
            "/api/auth/refresh",
            "/api/simple-auth/**",
            "/auth/**",
            "/webjars/**",
            "/favicon.ico",
            "/error",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/v3/api-docs/**"
    );
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // 检查是否是白名单路径
        if (isWhiteListPath(path)) {
            log.debug("Path in whitelist, skipping authentication: {}", path);
            return chain.filter(exchange);
        }
        
        // 检查请求属性中是否标记为Swagger UI请求
        if (Boolean.TRUE.equals(exchange.getAttribute("isSwaggerUIRequest"))) {
            log.debug("Swagger UI request detected, skipping authentication: {}", path);
            return chain.filter(exchange);
        }
        
        // 获取Authorization头
        List<String> authHeaders = exchange.getRequest().getHeaders().get("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            log.debug("Missing authorization header for path: {}", path);
            return unauthorizedResponse(exchange, "Missing authorization header");
        }
        
        String authHeader = authHeaders.get(0);
        if (!authHeader.startsWith("Bearer ")) {
            log.debug("Invalid authorization header format for path: {}", path);
            return unauthorizedResponse(exchange, "Invalid authorization header format");
        }
        
        String token = authHeader.substring(7);
        UserToken userToken = userService.validateToken(token);
        
        if (userToken == null) {
            log.debug("Invalid or expired token for path: {}", path);
            return unauthorizedResponse(exchange, "Invalid or expired token");
        }
        
        // 将用户信息添加到请求属性
        exchange.getAttributes().put("userId", userToken.getUserId());
        exchange.getAttributes().put("username", userToken.getUsername());
        
        return chain.filter(exchange);
    }
    
    private boolean isWhiteListPath(String path) {
        return whiteList.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
    
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        
        Map<String, String> response = new HashMap<>();
        response.put("error", "Unauthorized");
        response.put("message", message);
        
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(response);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        } catch (Exception e) {
            log.error("Error writing unauthorized response", e);
            return Mono.error(e);
        }
    }
} 