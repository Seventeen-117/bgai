package com.bgpay.bgai.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * 应用测试配置类
 * 通过设置系统属性来禁用各种不需要的自动配置
 */
@TestConfiguration
public class ApplicationTestConfig {

    /**
     * 在测试应用上下文初始化时，设置系统属性来禁用特定的自动配置
     */
    @PostConstruct
    public void init() {
        // 禁用RocketMQ相关配置
        System.setProperty("rocketmq.messageConsumer.enabled", "false");
        System.setProperty("rocketmq.producer.enable", "false");
        System.setProperty("rocketmq.name-server", "8.133.246.113:9876");
        System.setProperty("rocketmq.producer.group", "test-group");
        
        // 禁用Seata相关配置
        System.setProperty("seata.enabled", "false");
        System.setProperty("saga.enabled", "false");
        System.setProperty("seata.saga.state-machine.auto-register", "false");
        
        // 禁用Spring Cloud相关配置
        System.setProperty("spring.cloud.loadbalancer.enabled", "false");
        System.setProperty("spring.cloud.discovery.enabled", "false");
        System.setProperty("spring.cloud.config.enabled", "false");
        System.setProperty("spring.cloud.gateway.enabled", "false");
        System.setProperty("spring.cloud.circuitbreaker.resilience4j.enabled", "false");
        System.setProperty("spring.cloud.circuitbreaker.enabled", "false");
        
        // 禁用Bean验证配置
        System.setProperty("spring.validation.is-method-validation-bean-post-processor-enabled", "false");
        
        // 启用Bean定义覆盖
        System.setProperty("spring.main.allow-bean-definition-overriding", "true");
    }
} 