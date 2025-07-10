package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayReactiveLoadBalancerClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * 禁用主应用中的GatewayRouteConfig配置
 * 避免加载Spring Cloud Gateway相关自动配置
 */
@TestConfiguration
@Order(Ordered.HIGHEST_PRECEDENCE)
@EnableAutoConfiguration(exclude = {
    GatewayAutoConfiguration.class,
    GatewayReactiveLoadBalancerClientAutoConfiguration.class
})
@ComponentScan(
    basePackages = "com.bgpay.bgai.config",
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {GatewayRouteConfig.class}
        )
    }
)
@ConditionalOnProperty(name = "spring.cloud.gateway.enabled", havingValue = "false", matchIfMissing = false)
public class GatewayRouteConfigMock {
    
    /**
     * 空方法，仅用于标记该配置类被加载
     */
    @Bean
    public String gatewayDisableMarker() {
        return "Gateway configuration disabled for tests";
    }
} 