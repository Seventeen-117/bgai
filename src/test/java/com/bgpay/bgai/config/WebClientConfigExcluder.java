package com.bgpay.bgai.config;

import com.bgpay.bgai.web.WebClientConfig;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * 专用于排除主应用中的WebClientConfig
 * 结合@EnableAutoConfiguration和@Primary bean的方式
 * 确保测试环境不会使用主应用中的WebClientConfig
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    WebClientAutoConfiguration.class
})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebClientConfigExcluder {

    /**
     * 提供一个空实现的WebClientConfig
     * 使用@Primary确保覆盖主应用中的WebClientConfig
     */
    @Bean
    @Primary
    public WebClientConfig webClientConfig() {
        // 返回一个空实现，覆盖主应用的bean
        return new WebClientConfig() {
            // 所有方法都是空实现
        };
    }
} 