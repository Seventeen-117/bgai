package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * 禁用断路器相关的自动配置
 * 用于测试环境，避免自动配置与测试配置冲突
 */
@Configuration
@EnableAutoConfiguration(exclude = {})
public class CircuitBreakerAutoConfigurationDisabler {
    // 仅用于禁用自动配置，无需其他内容
    
    // 注意：由于断路器相关的自动配置类可能因依赖版本而异，
    // 我们在测试属性中禁用相关功能，而不是在这里排除具体的类
} 