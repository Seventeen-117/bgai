package com.bgpay.bgai.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

/**
 * 统一的环境配置类
 * 提供唯一的主要Environment实现，解决多个@Primary环境bean冲突问题
 */
@TestConfiguration
public class UnifiedEnvironmentConfig {

    /**
     * 提供唯一的主要Environment实现
     * 所有其他配置类都应该避免使用@Primary注解Environment
     */
    @Bean
    @Primary
    public ConfigurableEnvironment environment() {
        StandardEnvironment environment = new StandardEnvironment();
        
        // 配置测试环境中需要的系统属性
        System.setProperty("spring.main.allow-bean-definition-overriding", "true");
        System.setProperty("spring.validation.is-method-validation-bean-post-processor-enabled", "false");
        
        // RocketMQ相关配置
        System.setProperty("rocketmq.name-server", "8.133.246.113:9876");
        System.setProperty("rocketmq.producer.group", "test-group");
        System.setProperty("rocketmq.messageConsumer.enabled", "false");
        System.setProperty("rocketmq.producer.enable", "false");
        
        // 禁用其他组件
        System.setProperty("seata.enabled", "false");
        System.setProperty("saga.enabled", "false");
        System.setProperty("seata.saga.state-machine.auto-register", "false");
        System.setProperty("spring.cloud.loadbalancer.enabled", "false");
        System.setProperty("spring.cloud.discovery.enabled", "false");
        System.setProperty("spring.cloud.config.enabled", "false");
        System.setProperty("spring.cloud.gateway.enabled", "false");
        System.setProperty("spring.cloud.circuitbreaker.resilience4j.enabled", "false");
        System.setProperty("spring.cloud.circuitbreaker.enabled", "false");
        
        return environment;
    }
} 