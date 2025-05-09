package com.bgpay.bgai.config;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 请求属性提供者，用于在控制器中获取当前用户信息
 */
@Component
public class RequestAttributesProvider {
    
    /**
     * 从请求上下文中获取用户ID
     * 
     * @param exchange 服务器Web交换对象
     * @return 用户ID的Mono
     */
    public Mono<String> getUserId(ServerWebExchange exchange) {
        Object userId = exchange.getAttribute("userId");
        return userId != null ? Mono.just(userId.toString()) : Mono.empty();
    }
    
    /**
     * 从请求上下文中获取用户名
     * 
     * @param exchange 服务器Web交换对象
     * @return 用户名的Mono
     */
    public Mono<String> getUsername(ServerWebExchange exchange) {
        Object username = exchange.getAttribute("username");
        return username != null ? Mono.just(username.toString()) : Mono.empty();
    }
} 