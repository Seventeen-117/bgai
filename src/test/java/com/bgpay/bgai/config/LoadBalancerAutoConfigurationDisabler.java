package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * 禁用Spring Cloud LoadBalancer自动配置
 * 用于测试环境，避免自动配置与测试配置冲突
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration.class,
    org.springframework.cloud.loadbalancer.config.BlockingLoadBalancerClientAutoConfiguration.class,
    org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration.class
})
public class LoadBalancerAutoConfigurationDisabler {
    // 仅用于禁用自动配置，无需其他内容
} 