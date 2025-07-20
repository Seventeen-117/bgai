package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

/**
 * 测试环境RocketMQ配置类
 * 禁用RocketMQ相关自动配置，同时提供必要的ConfigurableEnvironment实现
 */
@Configuration
public class RocketMQTestConfig {

    /**
     * 提供ConfigurableEnvironment的实现
     * 这是RocketMQ的ListenerContainerConfiguration所需要的
     */
    @Bean(name = "rocketMQConfigEnvironment")
    @ConditionalOnMissingBean(name = "configurableEnvironment")
    public ConfigurableEnvironment rocketMQConfigEnvironment() {
        StandardEnvironment environment = new StandardEnvironment();
        
        // 设置RocketMQ相关属性，确保禁用RocketMQ相关功能
        System.setProperty("rocketmq.name-server", "8.133.246.113:9876");
        System.setProperty("rocketmq.producer.group", "test-group");
        System.setProperty("rocketmq.messageConsumer.enabled", "false");
        System.setProperty("rocketmq.producer.enable", "false");
        
        return environment;
    }
} 