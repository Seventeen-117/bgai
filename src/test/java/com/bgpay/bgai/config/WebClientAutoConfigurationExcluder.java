package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * 排除WebClient自动配置
 * 防止Spring Boot自动创建WebClient.Builder
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    WebClientAutoConfiguration.class
})
public class WebClientAutoConfigurationExcluder {
} 