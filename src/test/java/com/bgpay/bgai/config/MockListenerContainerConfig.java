package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

/**
 * 模拟RocketMQ的ListenerContainerConfiguration配置类
 * 解决RocketMQ自动配置导致的依赖问题
 */
@TestConfiguration
public class MockListenerContainerConfig {

    /**
     * 提供ConfigurableEnvironment，专门用于RocketMQ配置
     * 这是解决RocketMQ依赖问题的关键
     */
    @Bean
    @Primary
    public ConfigurableEnvironment environment() {
        StandardEnvironment environment = new StandardEnvironment();
        // 设置RocketMQ相关属性，确保禁用RocketMQ相关功能
        System.setProperty("rocketmq.name-server", "8.133.246.113:9876");
        System.setProperty("rocketmq.producer.group", "test-group");
        System.setProperty("rocketmq.messageConsumer.enabled", "false");
        System.setProperty("rocketmq.producer.enable", "false");
        return environment;
    }
} 