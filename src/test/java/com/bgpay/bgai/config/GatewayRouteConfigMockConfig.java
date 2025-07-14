package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Gateway路由配置的Mock实现，用于测试环境
 * 避免需要完整Spring Cloud Gateway环境
 */
@Configuration
public class GatewayRouteConfigMockConfig {

    /**
     * 提供一个Mock的RouteLocator bean，替代原始实现
     */
    @Bean
    @Primary
    public RouteLocator customRouteLocator() {
        return Mockito.mock(RouteLocator.class);
    }

    /**
     * 提供一个Mock的RouteLocatorBuilder bean，替代原始实现
     */
    @Bean
    @Primary
    public RouteLocatorBuilder routeLocatorBuilder() {
        return Mockito.mock(RouteLocatorBuilder.class);
    }
} 