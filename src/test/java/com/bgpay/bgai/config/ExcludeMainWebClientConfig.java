package com.bgpay.bgai.config;

import com.bgpay.bgai.web.WebClientConfig;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 明确排除主应用中的WebClientConfig类
 * 避免WebClientConfig导致的冲突
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration.class
})
public class ExcludeMainWebClientConfig {

    /**
     * 使用@MockBean标记WebClientConfig，防止Spring Boot创建真实的bean
     * 这个mock将被其他配置类中的具体实现替代
     */
    @MockBean
    private WebClientConfig webClientConfig;
} 