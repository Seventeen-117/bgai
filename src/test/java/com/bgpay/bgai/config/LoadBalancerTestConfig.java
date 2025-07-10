package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;

/**
 * 负载均衡测试配置
 * 提供负载均衡相关的mock实现
 */
@TestConfiguration
public class LoadBalancerTestConfig {

    /**
     * 提供LoadBalancerClient的mock实现
     */
    @Bean
    @Primary
    public LoadBalancerClient loadBalancerClient() {
        LoadBalancerClient mockClient = Mockito.mock(LoadBalancerClient.class);
        ServiceInstance mockInstance = Mockito.mock(ServiceInstance.class);
        
        // 配置mock行为
        Mockito.when(mockInstance.getUri()).thenReturn(URI.create("http://localhost:8080"));
        Mockito.when(mockClient.choose(Mockito.anyString())).thenReturn(mockInstance);
        
        return mockClient;
    }
    
    /**
     * 提供LoadBalancerClientFactory的mock实现
     */
    @Bean
    @Primary
    public LoadBalancerClientFactory loadBalancerClientFactory() {
        return Mockito.mock(LoadBalancerClientFactory.class);
    }
    
    /**
     * 提供ReactorLoadBalancerExchangeFilterFunction的mock实现
     */
    @Bean
    @Primary
    public ReactorLoadBalancerExchangeFilterFunction reactorLoadBalancerExchangeFilterFunction() {
        return Mockito.mock(ReactorLoadBalancerExchangeFilterFunction.class);
    }
    
    /**
     * 提供LoadBalancedExchangeFilterFunction的mock实现
     */
    @Bean
    @Primary
    public LoadBalancedExchangeFilterFunction loadBalancedExchangeFilterFunction() {
        return Mockito.mock(LoadBalancedExchangeFilterFunction.class);
    }
    
    /**
     * 提供带有负载均衡功能的WebClient.Builder
     */
    @Bean
    @Primary
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
} 