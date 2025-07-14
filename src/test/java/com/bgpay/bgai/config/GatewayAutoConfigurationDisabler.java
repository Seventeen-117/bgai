package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * 禁用Spring Cloud Gateway相关的自动配置
 * 在测试环境中使用，避免需要完整的Gateway环境
 */
@Configuration
@EnableAutoConfiguration(exclude = {
        org.springframework.cloud.gateway.config.GatewayAutoConfiguration.class,
        org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration.class
})
public class GatewayAutoConfigurationDisabler {
    // 这个类不需要其他内容，仅用于禁用自动配置
} 