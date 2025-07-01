package com.bgpay.bgai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway路由配置类
 * 提供Gateway的基础配置及部分默认路由
 */
@Slf4j
@Configuration
public class GatewayRouteConfig {

    /**
     * 创建默认路由定位器
     * 用于提供一些基础路由配置，与动态路由共存
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        log.info("初始化默认路由配置");
        return builder.routes()
                // 健康检查路由
                .route("health_check", r -> r.path("/health/**")
                        .uri("http://localhost:8688/health"))
                // 静态资源路由
                .route("static_resources", r -> r.path("/static/**")
                        .uri("http://localhost:8688"))
                .build();
    }
    
    /**
     * IP限流解析器
     * 基于请求IP进行限流
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            return Mono.just(ip);
        };
    }
    
    /**
     * 用户限流解析器
     * 基于用户ID进行限流
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null) {
                return Mono.just(userId);
            }
            return Mono.just("anonymous");
        };
    }
    
    /**
     * API限流解析器
     * 基于API Key进行限流
     */
    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
            if (apiKey != null) {
                return Mono.just(apiKey);
            }
            return Mono.just("anonymous");
        };
    }
    
    /**
     * Redis限流器配置
     */
    @Bean
    public RedisRateLimiter customRedisRateLimiter() {
        // 参数：默认请求数、默认突发请求数
        return new RedisRateLimiter(10, 20);
    }
} 