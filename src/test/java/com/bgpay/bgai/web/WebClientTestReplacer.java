package com.bgpay.bgai.web;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

/**
 * WebClient测试替换器
 * 在相同包路径下覆盖原有的WebClientConfig类中的bean定义
 */
@TestConfiguration
@Profile("test")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebClientTestReplacer {

    /**
     * 直接替代WebClientConfig中的同名方法，提供一个可用的测试WebClient
     */
    @Bean
    @Primary
    public WebClient webClient(WebClient.Builder builder) {
        // 使用注入的builder而不是自己创建，避免循环依赖
        return Mockito.mock(WebClient.class);
    }
    
    /**
     * 直接替代WebClientConfig中的同名方法，提供一个可用的测试WebClient.Builder
     */
    @Bean
    @Primary
    public WebClient.Builder webClientBuilder() {
        WebClient.Builder builder = Mockito.mock(WebClient.Builder.class);
        
        // 确保链式调用返回builder本身，而不是null
        Mockito.when(builder.baseUrl(Mockito.anyString())).thenReturn(builder);
        Mockito.when(builder.defaultHeader(Mockito.anyString())).thenReturn(builder);
        Mockito.when(builder.defaultHeader(Mockito.anyString(), Mockito.any(String.class))).thenReturn(builder);
        Mockito.when(builder.defaultHeader(Mockito.anyString(), Mockito.any(String[].class))).thenReturn(builder);
        Mockito.when(builder.defaultHeaders(Mockito.any(Consumer.class))).thenReturn(builder);
        Mockito.when(builder.defaultCookie(Mockito.anyString(), Mockito.anyString())).thenReturn(builder);
        Mockito.when(builder.filter(Mockito.any(ExchangeFilterFunction.class))).thenReturn(builder);
        Mockito.when(builder.exchangeStrategies(Mockito.any(ExchangeStrategies.class))).thenReturn(builder);
        Mockito.when(builder.codecs(Mockito.any(Consumer.class))).thenReturn(builder);
        
        // 重要: 创建一个独立的WebClient实例用于返回
        // 而不是调用webClient()方法，这样避免循环依赖
        WebClient mockWebClient = Mockito.mock(WebClient.class);
        Mockito.when(builder.build()).thenReturn(mockWebClient);
        
        return builder;
    }
    
    /**
     * 直接替代WebClientConfig中的同名方法，提供ExchangeStrategies
     */
    @Bean
    @Primary
    public ExchangeStrategies exchangeStrategies() {
        return Mockito.mock(ExchangeStrategies.class);
    }
} 