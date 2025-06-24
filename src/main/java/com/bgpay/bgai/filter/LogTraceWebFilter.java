package com.bgpay.bgai.filter;

import com.bgpay.bgai.utils.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 日志追踪WebFilter，用于WebFlux应用中记录请求的追踪信息
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LogTraceWebFilter implements WebFilter {
    private static final Logger logger = LoggerFactory.getLogger(LogTraceWebFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 为每个请求创建一个新的追踪ID
        LogUtils.startTrace();
        
        // 记录请求的基本信息
        String requestPath = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        String remoteAddress = exchange.getRequest().getRemoteAddress() != null ? 
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
        
        // 记录请求详情
        logger.debug("Processing request: {} {} from {}", method, requestPath, remoteAddress);
        
        // 从请求属性中获取用户ID (通常由认证过滤器设置)
        Object userId = exchange.getAttribute("userId");
        if (userId != null) {
            LogUtils.setUserId(userId.toString());
            logger.debug("Request from user: {}", userId);
        }
        
        return chain.filter(exchange)
                .doFinally(signal -> {
                    // 如果是异常信号，记录错误
                    if (signal.toString().equals("ERROR")) {
                        logger.error("Request processing failed with signal: {}", signal);
                    }
                    
                    // 记录响应状态
                    logger.debug("Completed request with status: {}", 
                            exchange.getResponse().getStatusCode() != null ? 
                                    exchange.getResponse().getStatusCode() : "unknown");
                    
                    // 请求完全处理完毕，清理追踪上下文
                    LogUtils.clearTrace();
                });
    }
} 