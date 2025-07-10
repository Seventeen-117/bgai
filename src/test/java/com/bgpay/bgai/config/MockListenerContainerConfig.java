package com.bgpay.bgai.config;

import org.apache.rocketmq.spring.autoconfigure.ListenerContainerConfiguration;
import org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
     * 提供模拟的ListenerContainerConfiguration
     * 避免RocketMQ自动配置创建真实实例
     */
    @Bean
    @Primary
    public ListenerContainerConfiguration listenerContainerConfiguration() {
        return Mockito.mock(ListenerContainerConfiguration.class);
    }
    
    /**
     * 提供ConfigurableEnvironment，专门用于RocketMQ配置
     * 这是解决RocketMQ依赖问题的关键
     */
    @Bean
    @ConditionalOnMissingBean(ConfigurableEnvironment.class)
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