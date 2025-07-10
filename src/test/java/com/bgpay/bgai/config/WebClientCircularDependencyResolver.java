package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 专门用于解决WebClient与WebClient.Builder之间的循环依赖问题
 * 使用最高优先级覆盖所有其他配置
 */
@TestConfiguration(proxyBeanMethods = false)
@Order(Ordered.HIGHEST_PRECEDENCE - 100) // 确保比其他配置优先级更高
public class WebClientCircularDependencyResolver {

    /**
     * 提供一个不依赖于WebClient.Builder的WebClient实现
     * 使用@Primary确保它覆盖其他所有WebClient bean
     */
    @Bean
    @Primary
    public WebClient webClient() {
        return Mockito.mock(WebClient.class);
    }

    /**
     * 提供一个不依赖于WebClient的WebClient.Builder实现
     * 使用@Primary确保它覆盖其他所有WebClient.Builder bean
     */
    @Bean
    @Primary
    public WebClient.Builder webClientBuilder() {
        WebClient.Builder builder = Mockito.mock(WebClient.Builder.class);
        
        // 确保基本方法链返回builder本身
        Mockito.when(builder.baseUrl(Mockito.anyString())).thenReturn(builder);
        Mockito.when(builder.filter(Mockito.any())).thenReturn(builder);
        
        // 创建独立的WebClient实例，不要依赖其他bean
        WebClient mockClient = Mockito.mock(WebClient.class);
        Mockito.when(builder.build()).thenReturn(mockClient);
        
        return builder;
    }
} 