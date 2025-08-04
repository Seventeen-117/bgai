package com.bgpay.bgai.filter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
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
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 接口签名验证过滤器
 * 实现HMAC-SHA256签名验证、时间戳验证和nonce防重放攻击
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 3) // 在API Key认证之后执行
public class SignatureVerificationFilter extends OncePerRequestFilter {

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

        // 如果签名验证被禁用，直接通过
        if (!signatureEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 提取签名参数
            String appId = request.getParameter("appId");
            String timestamp = request.getParameter("timestamp");
            String nonce = request.getParameter("nonce");
            String sign = request.getParameter("sign");

            // 验证必需参数
            if (StrUtil.isBlank(appId) || StrUtil.isBlank(timestamp) || 
                StrUtil.isBlank(nonce) || StrUtil.isBlank(sign)) {
                log.warn("Missing required signature parameters for path: {}", path);
                handleSignatureError(response, HttpStatus.BAD_REQUEST, "Missing required signature parameters");
                return;
            }

            // 验证时间戳
            if (!signatureVerificationService.validateTimestamp(timestamp, timestampExpireSeconds)) {
                log.warn("Timestamp expired for path: {}, timestamp: {}", path, timestamp);
                handleSignatureError(response, HttpStatus.UNAUTHORIZED, "Timestamp expired");
                return;
            }

            // 验证nonce防重放攻击
            if (!signatureVerificationService.validateNonce(nonce, nonceCacheExpireSeconds)) {
                log.warn("Nonce already used (replay attack detected) for path: {}, nonce: {}", path, nonce);
                handleSignatureError(response, HttpStatus.FORBIDDEN, "Replay attack detected");
                return;
            }

            // 获取所有请求参数
            Map<String, String> allParams = getAllRequestParameters(request);
            
            // 验证签名
            if (!signatureVerificationService.verifySignature(allParams, sign, appId)) {
                log.warn("Signature verification failed for path: {}, appId: {}", path, appId);
                handleSignatureError(response, HttpStatus.UNAUTHORIZED, "Signature verification failed");
                return;
            }

            // 保存nonce到缓存
            signatureVerificationService.saveNonce(nonce, nonceCacheExpireSeconds);

            log.debug("Signature verification passed for path: {}, appId: {}", path, appId);
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Error during signature verification for path: {}", path, e);
            handleSignatureError(response, HttpStatus.INTERNAL_SERVER_ERROR, "Signature verification error");
        }
    }

    /**
     * 检查是否为排除的路径
     */
    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith) || 
               path.contains("/public/");
    }

    /**
     * 获取所有请求参数
     */
    private Map<String, String> getAllRequestParameters(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        
        // 获取URL参数
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            if (StrUtil.isNotBlank(paramValue)) {
                params.put(paramName, paramValue);
            }
        }

        // 获取请求体参数（如果是POST/PUT请求）
        if (HttpMethod.POST.matches(request.getMethod()) || HttpMethod.PUT.matches(request.getMethod())) {
            try {
                // 这里可以扩展支持JSON请求体的签名验证
                // 目前先支持URL参数
            } catch (Exception e) {
                log.warn("Failed to parse request body parameters", e);
            }
        }

        return params;
    }

    /**
     * 处理签名验证错误
     */
    private void handleSignatureError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Signature verification failed");
        errorResponse.put("message", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
} 