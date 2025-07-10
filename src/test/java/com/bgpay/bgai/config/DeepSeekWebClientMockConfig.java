package com.bgpay.bgai.config;

import com.bgpay.bgai.service.deepseek.DeepSeekServiceImp;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * DeepSeekServiceImp WebClient配置类
 * 为DeepSeekServiceImp提供专用的WebClient实现
 */
@TestConfiguration
@Order(Ordered.HIGHEST_PRECEDENCE - 10) // 使用最高优先级确保覆盖其他配置
public class DeepSeekWebClientMockConfig {

    /**
     * 提供专用的WebClient实例给DeepSeekServiceImp使用
     * 解决多个primary WebClient.Builder的冲突
     * 使用@Primary确保它是唯一的主要WebClient实现
     */
    @Bean
    @Primary
    public WebClient webClient() {
        // 创建一个真实的WebClient而不是mock，这样可以避免一些空指针异常
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();

        // 创建一个配置良好的WebClient
        return WebClient.builder()
                .baseUrl("https://test-api.example.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(strategies)
                .build();
    }
    
    /**
     * 确保没有其他loadBalancedWebClient bean被注册为@Primary
     */
    @Bean(name = "loadBalancedWebClient")
    public WebClient loadBalancedWebClient() {
        return Mockito.mock(WebClient.class);
    }
}