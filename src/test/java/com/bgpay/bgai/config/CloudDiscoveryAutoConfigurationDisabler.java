package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * 禁用Spring Cloud Discovery相关的自动配置
 * 用于测试环境，避免自动配置与测试配置冲突
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration.class,
    org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClientAutoConfiguration.class
})
public class CloudDiscoveryAutoConfigurationDisabler {
    // 仅用于禁用自动配置，无需其他内容
} 