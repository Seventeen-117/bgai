package com.bgpay.bgai.interceptor;

import com.bgpay.bgai.utils.LogUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class LogTraceInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        LogUtils.startTrace();
        // 如果有用户认证信息，可以在这里设置
        // LogUtils.setUserId(getUserIdFromRequest(request));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LogUtils.clearTrace();
    }
} 