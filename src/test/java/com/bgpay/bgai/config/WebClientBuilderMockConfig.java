package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient.Builder的统一配置类
 * 处理多个@Primary WebClient.Builder bean的冲突
 * 完全覆盖主应用中的WebClientConfig，确保测试环境中只有唯一的WebClient.Builder实现
 */
@TestConfiguration
public class WebClientBuilderMockConfig {

    /**
     * 提供统一的WebClient.Builder mock实现
     * 使用@Primary确保它是唯一的主要实现
     */
    @Bean
    @Primary
    public WebClient.Builder webClientBuilder() {
        WebClient.Builder mockBuilder = Mockito.mock(WebClient.Builder.class);
        WebClient mockWebClient = Mockito.mock(WebClient.class);
        
        // 配置mock行为
        Mockito.when(mockBuilder.build()).thenReturn(mockWebClient);
        Mockito.when(mockBuilder.baseUrl(Mockito.anyString())).thenReturn(mockBuilder);
        Mockito.when(mockBuilder.filter(Mockito.any())).thenReturn(mockBuilder);
        
        return mockBuilder;
    }
    
    /**
     * 提供WebClient的mock实现，覆盖主应用中的WebClient bean
     */
    @Bean
    @Primary
    public WebClient webClient() {
        return Mockito.mock(WebClient.class);
    }
    
    /**
     * 提供loadBalancedWebClient的mock实现
     * 覆盖主应用中的loadBalancedWebClient bean
     */
    @Bean(name = "loadBalancedWebClient")
    @Primary
    public WebClient loadBalancedWebClient() {
        return Mockito.mock(WebClient.class);
    }
    
    /**
     * 提供discoveryWebClientBuilder的mock实现
     * 不使用@Primary注解，避免冲突
     */
    @Bean(name = "discoveryWebClientBuilder")
    public WebClient.Builder discoveryWebClientBuilder() {
        WebClient.Builder mockBuilder = Mockito.mock(WebClient.Builder.class);
        WebClient mockWebClient = Mockito.mock(WebClient.class);
        
        // 配置mock行为
        Mockito.when(mockBuilder.build()).thenReturn(mockWebClient);
        Mockito.when(mockBuilder.baseUrl(Mockito.anyString())).thenReturn(mockBuilder);
        Mockito.when(mockBuilder.filter(Mockito.any())).thenReturn(mockBuilder);
        
        return mockBuilder;
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
     * 提供LoadBalancerClient的mock实现
     */
    @Bean
    @Primary
    public LoadBalancerClient loadBalancerClient() {
        return Mockito.mock(LoadBalancerClient.class);
    }
} 