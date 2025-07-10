package com.bgpay.bgai.config;

import com.bgpay.bgai.service.ApiConfigService;
import com.bgpay.bgai.service.UserService;
import com.bgpay.bgai.service.deepseek.DeepSeekService;
import com.bgpay.bgai.service.deepseek.ReactiveFileProcessor;
import com.bgpay.bgai.service.impl.FallbackService;
import com.bgpay.bgai.transaction.TransactionCoordinator;
import com.bgpay.bgai.web.RequestAttributesProvider;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * ReactiveChatController所需的mock配置
 * 提供ReactiveChatController所需的所有依赖
 */
@TestConfiguration
public class ReactiveChatControllerMockConfig {

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
     * 提供ReactiveFileProcessor的mock实现
     */
    @Bean
    @Primary
    public ReactiveFileProcessor reactiveFileProcessor() {
        return Mockito.mock(ReactiveFileProcessor.class);
    }
    
    /**
     * 提供FallbackService的mock实现
     */
    @Bean
    @Primary
    public FallbackService fallbackService() {
        return Mockito.mock(FallbackService.class);
    }
    
    /**
     * 提供RequestAttributesProvider的mock实现
     */
    @Bean
    @Primary
    public RequestAttributesProvider requestAttributesProvider() {
        return Mockito.mock(RequestAttributesProvider.class);
    }
} 