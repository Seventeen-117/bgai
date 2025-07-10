package com.bgpay.bgai.config;

import com.bgpay.bgai.web.WebClientConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * WebClientBean排除配置
 * 专门用于替换和移除原始WebClientConfig中的WebClient bean
 */
@TestConfiguration
public class WebClientBeanExclusionConfig {
    
    /**
     * 这个bean返回一个空对象，用于替换原始WebClientConfig bean
     * 通过在配置中使用@Primary标记，确保这个bean会被优先使用
     * 并且原始WebClientConfig不会被创建或注入到其他bean中
     */
    @Bean
    @Primary
    public WebClientConfig webClientConfig() {
        // 返回一个空实现，避免原始bean的初始化
        return new WebClientConfig() {
            // 空实现，所有方法都会被mock替代
        };
    }
} 