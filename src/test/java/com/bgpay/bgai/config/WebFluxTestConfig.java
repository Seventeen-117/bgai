package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * WebFlux测试综合配置类
 * 提供WebFlux测试所需的所有配置
 */
@TestConfiguration
public class WebFluxTestConfig {

    /**
     * 提供ReactiveCircuitBreakerFactory的mock实现
     */
    @Bean
    @Primary
    public ReactiveCircuitBreakerFactory reactiveCircuitBreakerFactory() {
        // 创建一个mock实现
        ReactiveCircuitBreakerFactory mockFactory = Mockito.mock(ReactiveCircuitBreakerFactory.class);
        ReactiveCircuitBreaker mockBreaker = Mockito.mock(ReactiveCircuitBreaker.class);
        
        // 配置mock行为，让run方法直接返回传入的Mono
        Mockito.when(mockFactory.create(Mockito.anyString())).thenReturn(mockBreaker);
        Mockito.when(mockBreaker.run(Mockito.any(Mono.class), Mockito.any(Function.class)))
               .thenAnswer(invocation -> {
                   Mono<?> mono = invocation.getArgument(0);
                   return mono;
               });
        
        return mockFactory;
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
     * 提供WebClient.Builder的实现
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
     * 提供WebClient的实现
     */
    @Bean
    @Primary
    public WebClient webClient() {
        return Mockito.mock(WebClient.class);
    }
} 