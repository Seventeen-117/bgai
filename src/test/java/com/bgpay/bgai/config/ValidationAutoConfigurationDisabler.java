package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * 禁用Spring Boot Validation自动配置
 * 解决MethodValidationPostProcessor创建问题
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    ValidationAutoConfiguration.class
})
public class ValidationAutoConfigurationDisabler {
    // 仅用于禁用自动配置，无需其他内容
} 