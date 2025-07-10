package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.ClientHttpConnectorAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 禁用WebClient相关的所有自动配置
 * 用于测试环境，避免自动配置与测试配置冲突
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    WebClientAutoConfiguration.class,
    ClientHttpConnectorAutoConfiguration.class
})
public class WebClientAutoConfigurationDisabler {
    // 仅用于禁用自动配置，无需其他内容
    
    /**
     * 提供一个标记bean，表明WebClientAutoConfiguration被禁用
     */
    @Bean
    public WebClientDisabledMarker webClientDisabledMarker() {
        return new WebClientDisabledMarker();
    }
    
    /**
     * 标记类，表示WebClientAutoConfiguration已被禁用
     */
    public static class WebClientDisabledMarker {
        // 空实现
    }
} 