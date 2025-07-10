package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.mockito.Mockito;
import java.util.function.Consumer;

/**
 * WebClientConfig完全覆盖配置
 * 提供WebClient的完整测试替代，完全覆盖原有配置
 */
@TestConfiguration
@Order(Ordered.HIGHEST_PRECEDENCE) // 确保最高优先级
public class WebClientConfigOverrideConfig {
    
    /**
     * 提供主WebClient bean，代替原始实现
     */
    @Bean
    @Primary
    public WebClient webClient() {
        return Mockito.mock(WebClient.class);
    }
    
    /**
     * 提供主WebClient.Builder bean，代替原始实现
     */
    @Bean
    @Primary
    public WebClient.Builder webClientBuilder() {
        WebClient.Builder builder = Mockito.mock(WebClient.Builder.class);
        WebClient webClient = webClient();
        
        // 确保所有调用链都返回builder本身，而不是null
        Mockito.when(builder.baseUrl(Mockito.anyString())).thenReturn(builder);
        Mockito.when(builder.defaultHeader(Mockito.anyString())).thenReturn(builder);
        Mockito.when(builder.defaultHeader(Mockito.anyString(), Mockito.any(String.class))).thenReturn(builder);
        Mockito.when(builder.defaultHeader(Mockito.anyString(), Mockito.any(String[].class))).thenReturn(builder);
        Mockito.when(builder.defaultHeaders(Mockito.any(Consumer.class))).thenReturn(builder);
        Mockito.when(builder.defaultCookie(Mockito.anyString(), Mockito.anyString())).thenReturn(builder);
        Mockito.when(builder.filter(Mockito.any(ExchangeFilterFunction.class))).thenReturn(builder);
        Mockito.when(builder.exchangeStrategies(Mockito.any(ExchangeStrategies.class))).thenReturn(builder);
        Mockito.when(builder.codecs(Mockito.any(Consumer.class))).thenReturn(builder);
        
        // build()方法返回mock的WebClient
        Mockito.when(builder.build()).thenReturn(webClient);
        
        return builder;
    }

    /**
     * 提供Exchange策略
     */
    @Bean
    @Primary
    public ExchangeStrategies exchangeStrategies() {
        ExchangeStrategies strategies = Mockito.mock(ExchangeStrategies.class);
        return strategies;
    }
} 