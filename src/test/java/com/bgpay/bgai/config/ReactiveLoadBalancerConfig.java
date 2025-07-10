package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * ReactiveLoadBalancer配置类
 * 提供ReactiveLoadBalancer相关的bean
 */
@TestConfiguration
public class ReactiveLoadBalancerConfig {

    /**
     * 提供ReactorLoadBalancerExchangeFilterFunction的mock实现
     */
    @Bean
    @Primary
    public ReactorLoadBalancerExchangeFilterFunction reactorLoadBalancerExchangeFilterFunction() {
        return Mockito.mock(ReactorLoadBalancerExchangeFilterFunction.class);
    }
    
    /**
     * 提供WebClient.Builder的实现
     * 不使用@Primary注解，避免与WebClientBuilderMockConfig冲突
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
} 