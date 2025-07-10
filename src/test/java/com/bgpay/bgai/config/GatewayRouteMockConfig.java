package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.BooleanSpec;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * Gateway路由相关组件的Mock配置
 * 提供测试环境所需的Spring Cloud Gateway相关组件
 */
@TestConfiguration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayRouteMockConfig {

    /**
     * 提供RouteLocatorBuilder的Mock实现
     */
    @Bean
    @Primary
    public RouteLocatorBuilder routeLocatorBuilder() {
        // 创建一个简单的mock
        return Mockito.mock(RouteLocatorBuilder.class);
    }

    /**
     * 提供RouteLocator的Mock实现
     */
    @Bean
    @ConditionalOnMissingBean
    public RouteLocator routeLocator() {
        // 创建一个简单的mock
        return Mockito.mock(RouteLocator.class);
    }
    
    /**
     * 提供默认的IP限流解析器
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just("test-ip");
    }
    
    /**
     * 提供用户限流解析器
     */
    @Bean
    @ConditionalOnMissingBean(name = "userKeyResolver")
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.just("test-user");
    }
    
    /**
     * 提供API限流解析器
     */
    @Bean
    @ConditionalOnMissingBean(name = "apiKeyResolver")
    public KeyResolver apiKeyResolver() {
        return exchange -> Mono.just("test-api-key");
    }
} 