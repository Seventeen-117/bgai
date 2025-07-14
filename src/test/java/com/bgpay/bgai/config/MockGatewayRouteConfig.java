package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;

/**
 * Gateway路由配置的Mock替代实现
 * 使用与原始GatewayRouteConfig相同的bean名称，但提供mock实现
 */
@Configuration
@Order(Integer.MIN_VALUE) // 确保最高优先级
public class MockGatewayRouteConfig {

    /**
     * 提供一个Mock的RouteLocator bean，替代原始实现
     */
    @Bean
    @Primary
    public RouteLocator customRouteLocator() {
        return Mockito.mock(RouteLocator.class);
    }

    /**
     * IP限流解析器的Mock实现
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just("mock-ip");
    }
    
    /**
     * 用户限流解析器的Mock实现
     */
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.just("mock-user");
    }
    
    /**
     * API限流解析器的Mock实现
     */
    @Bean
    @Primary
    public KeyResolver apiKeyResolver() {
        return exchange -> Mono.just("mock-api");
    }
    
    /**
     * Redis限流器配置的Mock实现
     */
    @Bean
    @Primary
    public RedisRateLimiter customRedisRateLimiter() {
        return Mockito.mock(RedisRateLimiter.class);
    }
} 