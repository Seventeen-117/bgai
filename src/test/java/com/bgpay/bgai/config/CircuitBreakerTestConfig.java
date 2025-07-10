package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * 断路器测试配置类
 * 提供ReactiveCircuitBreakerFactory的mock实现
 */
@TestConfiguration
public class CircuitBreakerTestConfig {

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
} 