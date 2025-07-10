package com.bgpay.bgai.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClientBuilderUnifier 配置类
 * 专门解决测试环境中多个@Primary WebClient.Builder实例的冲突问题
 * 通过提供最高优先级的单一实例覆盖其他所有定义
 */
@TestConfiguration(proxyBeanMethods = false)
@Order(Ordered.HIGHEST_PRECEDENCE) // 使用最高优先级，但高于WebClientTestOverrideConfig
public class WebClientBuilderUnifier {

    /**
     * 提供统一的WebClient.Builder实现
     * 确保系统中只有一个带@Primary的WebClient.Builder
     */
    @Bean
    @Primary
    public WebClient.Builder webClientBuilder() {
        // 创建实际的WebClient.Builder而不是mock
        return WebClient.builder();
    }
    
    /**
     * 确保loadBalancedWebClientBuilder也由我们提供
     * 避免与其他定义冲突
     */
    @Bean(name = "loadBalancedWebClientBuilder")
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
    
    /**
     * 确保discoveryWebClientBuilder也由我们提供
     * 避免与其他定义冲突
     */
    @Bean(name = "discoveryWebClientBuilder")
    public WebClient.Builder discoveryWebClientBuilder() {
        return WebClient.builder();
    }
} 