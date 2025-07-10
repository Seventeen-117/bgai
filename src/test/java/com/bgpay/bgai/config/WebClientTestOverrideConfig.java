package com.bgpay.bgai.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 专门用于测试环境的WebClient配置
 * 使用较高的优先级@Order值，确保它覆盖原始的WebClientConfig
 */
@TestConfiguration(proxyBeanMethods = false)
@Order(-2147483648) // Integer.MIN_VALUE，确保最高优先级
public class WebClientTestOverrideConfig {

    /**
     * 提供唯一的loadBalancedWebClient bean
     * 直接使用构建器创建，不依赖注入WebClient.Builder
     */
    @Bean(name = "loadBalancedWebClient")
    @Primary
    public WebClient loadBalancedWebClient() {
        // 直接创建WebClient实例，避免依赖注入导致的冲突
        return WebClient.builder().build();
    }
} 