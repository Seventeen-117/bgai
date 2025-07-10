package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

/**
 * WebClient完整模拟配置
 * 提供完整的WebClient相关模拟实现，避免依赖真实的网络请求
 */
@TestConfiguration
public class WebClientMockConfiguration {

    /**
     * 提供一个模拟的WebClient.Builder
     */
    @Bean
    @Primary
    public WebClient.Builder webClientBuilder() {
        WebClient.Builder mockBuilder = Mockito.mock(WebClient.Builder.class);
        
        // 设置builder方法链的模拟行为 - 重要：每个方法都必须返回builder自身而不是null
        Mockito.when(mockBuilder.baseUrl(Mockito.anyString())).thenReturn(mockBuilder);
        Mockito.when(mockBuilder.defaultHeader(Mockito.anyString())).thenReturn(mockBuilder);
        Mockito.when(mockBuilder.defaultHeader(Mockito.anyString(), Mockito.any(String.class))).thenReturn(mockBuilder);
        Mockito.when(mockBuilder.defaultHeader(Mockito.anyString(), Mockito.any(String[].class))).thenReturn(mockBuilder);
        Mockito.when(mockBuilder.defaultHeaders(Mockito.any(Consumer.class))).thenReturn(mockBuilder);
        Mockito.when(mockBuilder.defaultCookie(Mockito.anyString(), Mockito.anyString())).thenReturn(mockBuilder);
        Mockito.when(mockBuilder.filter(Mockito.any(ExchangeFilterFunction.class))).thenReturn(mockBuilder);
        Mockito.when(mockBuilder.exchangeStrategies(Mockito.any(ExchangeStrategies.class))).thenReturn(mockBuilder);
        Mockito.when(mockBuilder.codecs(Mockito.any(Consumer.class))).thenReturn(mockBuilder);
        
        // 设置build方法返回模拟的WebClient
        WebClient mockWebClient = Mockito.mock(WebClient.class);
        Mockito.when(mockBuilder.build()).thenReturn(mockWebClient);
        
        return mockBuilder;
    }

    /**
     * 提供一个模拟的WebClient
     */
    @Bean
    @Primary
    public WebClient webClient(WebClient.Builder builder) {
        // 使用我们配置好的builder来构建WebClient
        return builder.build();
    }
    
    /**
     * 提供模拟的ExchangeStrategies
     */
    @Bean
    @Primary
    public ExchangeStrategies exchangeStrategies() {
        ExchangeStrategies mockStrategies = Mockito.mock(ExchangeStrategies.class);
        return mockStrategies;
    }
    
    /**
     * 提供模拟的ClientCodecConfigurer
     */
    @Bean
    @Primary
    public ClientCodecConfigurer clientCodecConfigurer() {
        return Mockito.mock(ClientCodecConfigurer.class);
    }
} 