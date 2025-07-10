package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient测试配置类
 * 提供WebClient相关的mock实现，解决多个primary bean的冲突
 */
@TestConfiguration
public class WebClientTestConfig {

    /**
     * 提供WebClient的mock实现
     */
    @Bean
    @Primary
    public WebClient webClient() {
        return Mockito.mock(WebClient.class);
    }
    
    /**
     * 提供WebClient.Builder的mock实现
     * 不使用@Primary注解，避免与WebClientBuilderMockConfig冲突
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        WebClient.Builder mockBuilder = Mockito.mock(WebClient.Builder.class);
        WebClient mockWebClient = Mockito.mock(WebClient.class);
        
        // 配置mock行为
        Mockito.when(mockBuilder.build()).thenReturn(mockWebClient);
        Mockito.when(mockBuilder.baseUrl(Mockito.anyString())).thenReturn(mockBuilder);
        Mockito.when(mockBuilder.filter(Mockito.any())).thenReturn(mockBuilder);
        
        return mockBuilder;
    }
} 