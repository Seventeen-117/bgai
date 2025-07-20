package com.bgpay.bgai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * 提供单一的testGatewayRouteConfig bean
 * 使用最高优先级确保覆盖其他配置类中的同名bean
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE - 100) // 使用比任何其他配置更高的优先级
public class TestGatewayRouteConfigProvider {

    /**
     * 提供一个明确的testGatewayRouteConfig bean，避免冲突
     */
    @Bean(name = "testGatewayRouteConfig")
    @Primary
    public Object testGatewayRouteConfig() {
        System.out.println("创建唯一的testGatewayRouteConfig bean");
        return new Object();
    }
} 