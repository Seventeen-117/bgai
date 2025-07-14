package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;

/**
 * 测试环境使用的Gateway路由配置
 * 完全替代主应用的GatewayRouteConfig，确保测试环境中不会抛出错误
 * 
 * 注意：此类已被禁用，所有功能已迁移到CompleteGatewayDisablingConfig
 */
// @Configuration 已禁用此配置类
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DisabledTestGatewayRouteConfig {

    /**
     * 创建mock的路由定位器，避免依赖外部资源
     */
    @Bean
    @Primary
    public RouteLocator customRouteLocator() {
        return Mockito.mock(RouteLocator.class);
    }
    
    /**
     * Mock的IP限流解析器
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just("test-ip");
    }
    
    /**
     * Mock的用户限流解析器
     */
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.just("test-user");
    }
    
    /**
     * Mock的API限流解析器
     */
    @Bean
    @Primary
    public KeyResolver apiKeyResolver() {
        return exchange -> Mono.just("test-api");
    }
    
    /**
     * Mock的Redis限流器
     */
    @Bean
    @Primary
    public RedisRateLimiter customRedisRateLimiter() {
        return Mockito.mock(RedisRateLimiter.class);
    }
    
    /**
     * 确保当前bean被实例化
     */
    public DisabledTestGatewayRouteConfig() {
        System.out.println("=== DisabledTestGatewayRouteConfig has been initialized but should not be used ===");
    }
} 