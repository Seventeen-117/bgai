package com.bgpay.bgai.config;

import org.apache.http.HttpHeaders;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * WebClientConfig完全替代配置类
 * 提供一个完整的WebClientConfig替代实现，用于测试环境
 */
@TestConfiguration(proxyBeanMethods = false)
@Order(Ordered.HIGHEST_PRECEDENCE + 20)  // 优先级中等，在其他配置之后
public class WebClientConfigMock {

    /**
     * 提供一个完全可用的WebClient实例，而不是简单的mock
     * 解决WebClientConfig.webClient中的NullPointerException问题
     */
    @Bean
    @Primary
    public WebClient webClient() {
        // 创建实际的ExchangeStrategies以避免NullPointerException
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();

        // 创建一个非mock的WebClient
        return WebClient.builder()
                .baseUrl("https://test-api.example.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(strategies)
                .filter(logRequest())
                .build();
    }

    /**
     * 提供loadBalancedWebClient实例
     */
    @Bean(name = "loadBalancedWebClient") 
    @Primary
    public WebClient loadBalancedWebClient() {
        return webClient(); // 返回相同的实例以简化
    }

    /**
     * 提供WebClient.Builder实例，确保是真实可用的
     */
    @Bean
    @Primary
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
    
    /**
     * 提供服务发现的WebClient构建器
     */
    @Bean
    @Primary
    public WebClient.Builder discoveryWebClientBuilder() {
        return WebClient.builder();
    }
    
    /**
     * 复制原始日志请求过滤器的功能
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            // 无操作的日志记录过滤器
            return Mono.just(clientRequest);
        });
    }
} 