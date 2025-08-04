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
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 异步签名验证过滤器
 * 演示不同的验证模式：完全异步、混合验证、快速验证
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 4) // 在同步验证过滤器之后执行
public class AsyncSignatureVerificationFilter extends OncePerRequestFilter {

    private final SignatureVerificationService signatureVerificationService;
    private final ObjectMapper objectMapper;

    @Value("${bgai.signature.enabled:true}")
    private boolean signatureEnabled;

    @Value("${bgai.signature.async.enabled:false}")
    private boolean asyncEnabled;

    @Value("${bgai.signature.async.mode:HYBRID}")
    private String asyncMode;

    @Value("${bgai.signature.async.timeout:5000}")
    private long asyncTimeout;

    // 不需要签名验证的路径
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
            "/api/signature/",
            "/api/app-secret/",
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
        if (!signatureEnabled || !asyncEnabled) {
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

            // 获取所有请求参数
            Map<String, String> allParams = getAllRequestParameters(request);
            
            // 根据配置选择验证模式
            boolean verificationResult = performAsyncVerification(allParams, sign, appId, path);
            
            if (!verificationResult) {
                log.warn("Async signature verification failed for path: {}, appId: {}", path, appId);
                handleSignatureError(response, HttpStatus.UNAUTHORIZED, "Async signature verification failed");
                return;
            }

            // 异步保存nonce到缓存
            signatureVerificationService.saveNonceAsync(nonce, 1800);

            log.debug("Async signature verification passed for path: {}, appId: {}", path, appId);
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Error during async signature verification for path: {}", path, e);
            handleSignatureError(response, HttpStatus.INTERNAL_SERVER_ERROR, "Async signature verification error");
        }
    }

    /**
     * 执行异步验证
     */
    private boolean performAsyncVerification(Map<String, String> params, String sign, String appId, String path) {
        try {
            CompletableFuture<Boolean> verificationFuture;
            
            switch (asyncMode.toUpperCase()) {
                case "ASYNC":
                    // 完全异步模式
                    verificationFuture = signatureVerificationService.verifySignatureAsync(params, sign, appId);
                    break;
                    
                case "HYBRID":
                    // 混合验证模式
                    verificationFuture = signatureVerificationService.verifySignatureFast(params, sign, appId);
                    break;
                    
                case "QUICK":
                    // 快速验证模式
                    boolean quickResult = signatureVerificationService.verifySignatureQuick(params, appId);
                    if (quickResult) {
                        // 快速验证通过，启动异步详细验证
                        signatureVerificationService.verifySignatureAsync(params, sign, appId)
                                .thenAccept(detailedResult -> {
                                    if (!detailedResult) {
                                        log.warn("Async detailed verification failed for appId: {}", appId);
                                    }
                                });
                    }
                    return quickResult;
                    
                default:
                    // 默认使用混合模式
                    verificationFuture = signatureVerificationService.verifySignatureFast(params, sign, appId);
                    break;
            }
            
            // 等待异步验证结果，设置超时时间
            return verificationFuture.get(asyncTimeout, TimeUnit.MILLISECONDS);
            
        } catch (Exception e) {
            log.error("Error during async verification for appId: {}", appId, e);
            return false;
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

        return params;
    }

    /**
     * 处理签名验证错误
     */
    private void handleSignatureError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Async signature verification failed");
        errorResponse.put("message", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("asyncMode", asyncMode);
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
} 