package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * 测试环境WebFlux配置类
 * 提供测试所需的WebClient和LoadBalancer相关组件
 */
@TestConfiguration
public class TestWebFluxConfig {

    /**
     * 提供ReactorLoadBalancerExchangeFilterFunction的mock实现
     * 解决测试环境中"Consider defining a bean of type 'ReactorLoadBalancerExchangeFilterFunction'"错误
     */
    @Bean
    @Primary
    public ReactorLoadBalancerExchangeFilterFunction reactorLoadBalancerExchangeFilterFunction() {
        // 创建一个mock实现，在测试环境中不需要真正的负载均衡功能
        return Mockito.mock(ReactorLoadBalancerExchangeFilterFunction.class);
    }
    
    /**
     * 提供测试环境使用的WebClient.Builder
     */
    @Bean
    @Primary
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .filter(logRequest());
    }
    
    /**
     * 创建一个简单的日志过滤器，用于记录测试请求
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            System.out.println("Test request: " + clientRequest.method() + " " + clientRequest.url());
            return Mono.just(clientRequest);
        });
    }
} 