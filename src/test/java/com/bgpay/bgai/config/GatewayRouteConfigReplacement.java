package com.bgpay.bgai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * GatewayRouteConfig的替代类
 * 用于测试环境，提供最简单的实现，避免加载真实的Gateway配置
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayRouteConfigReplacement {

    /**
     * 提供一个简单的gatewayRouteConfig bean
     */
    @Bean(name = "gatewayRouteConfig")
    @Primary
    public Object gatewayRouteConfig() {
        return new Object();
    }
    
    /**
     * 提供一个简单的testGatewayRouteConfig bean
     */
    @Bean(name = "testGatewayRouteConfig")
    @Primary
    public Object testGatewayRouteConfig() {
        return new Object();
    }
} 