package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient的Mock配置类
 * 用于测试环境中提供WebClient的mock实现
 */
@TestConfiguration
public class MockWebClientConfig {

    /**
     * 提供WebClient的mock实例
     */
    @Bean
    @Primary
    public WebClient webClient() {
        return Mockito.mock(WebClient.class);
    }
    
    /**
     * 提供WebClient.Builder的mock实例
     * 不使用@Primary注解，避免与WebClientBuilderMockConfig冲突
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        WebClient.Builder mockBuilder = Mockito.mock(WebClient.Builder.class);
        WebClient mockWebClient = Mockito.mock(WebClient.class);
        
        // 配置mock行为，使builder.build()返回mockWebClient
        Mockito.when(mockBuilder.build()).thenReturn(mockWebClient);
        Mockito.when(mockBuilder.baseUrl(Mockito.anyString())).thenReturn(mockBuilder);
        Mockito.when(mockBuilder.filter(Mockito.any())).thenReturn(mockBuilder);
        
        return mockBuilder;
    }
} 