package com.bgpay.bgai.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

/**
 * 测试环境ApplicationEnvironment配置
 * 提供标准的ConfigurableEnvironment实现
 */
@Configuration
public class TestApplicationEnvironmentConfig {

    /**
     * 提供标准的ApplicationEnvironment
     * 这是ExtConsumerResetConfiguration等RocketMQ相关配置所需要的
     */
    @Bean
    @ConditionalOnMissingBean(ConfigurableEnvironment.class)
    public ConfigurableEnvironment configurableEnvironment() {
        StandardEnvironment environment = new StandardEnvironment();
        
        // 设置RocketMQ相关属性
        System.setProperty("rocketmq.name-server", "8.133.246.113:9876");
        System.setProperty("rocketmq.producer.group", "test-group");
        System.setProperty("rocketmq.messageConsumer.enabled", "false");
        System.setProperty("rocketmq.producer.enable", "false");
        
        return environment;
    }
} 