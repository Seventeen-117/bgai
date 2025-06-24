package com.bgpay.bgai.interceptor;

import com.bgpay.bgai.utils.LogUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 日志追踪拦截器，用于记录请求的追踪信息
 */
@Component
public class LogTraceInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(LogTraceInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 为每个请求创建一个新的追踪ID
        LogUtils.startTrace();
        
        // 记录请求的基本信息
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String remoteAddr = request.getRemoteAddr();
        
        // 记录请求详情
        logger.debug("Processing request: {} {} from {}", method, requestURI, remoteAddr);
        
        // 从请求属性中获取用户ID (通常由认证过滤器设置)
        Object userId = request.getAttribute("userId");
        if (userId != null) {
            LogUtils.setUserId(userId.toString());
            logger.debug("Request from user: {}", userId);
        }
        
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // 请求处理完成后，记录响应状态
        logger.debug("Completed request with status: {}", response.getStatus());
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 如果有异常发生，记录异常信息
        if (ex != null) {
            logger.error("Request processing failed", ex);
        }
        
        // 请求完全处理完毕，清理追踪上下文
        LogUtils.clearTrace();
    }
} 