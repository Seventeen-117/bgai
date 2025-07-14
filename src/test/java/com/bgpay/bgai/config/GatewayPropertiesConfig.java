package com.bgpay.bgai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Gateway属性配置类
 * 在测试环境中提供禁用Gateway相关功能的属性
 */
@Configuration
public class GatewayPropertiesConfig {

    /**
     * 添加系统属性以完全禁用Gateway功能
     */
    @Bean
    public static MapPropertySource gatewayDisablingProperties(ConfigurableEnvironment env) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.cloud.gateway.enabled", false);
        properties.put("spring.cloud.gateway.discovery.locator.enabled", false);
        properties.put("spring.cloud.loadbalancer.enabled", false);
        properties.put("spring.cloud.discovery.enabled", false);
        
        // 添加要禁用的自动配置类
        String excludes = (String) env.getProperty("spring.autoconfigure.exclude", "");
        String additionalExcludes = "org.springframework.cloud.gateway.config.GatewayAutoConfiguration," +
                "org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration," +
                "org.springframework.cloud.gateway.config.GatewayMetricsAutoConfiguration";
                
        if (!excludes.isEmpty()) {
            additionalExcludes = excludes + "," + additionalExcludes;
        }
        properties.put("spring.autoconfigure.exclude", additionalExcludes);
        
        MapPropertySource source = new MapPropertySource("gateway-disabling-properties", properties);
        env.getPropertySources().addFirst(source);
        return source;
    }
} 