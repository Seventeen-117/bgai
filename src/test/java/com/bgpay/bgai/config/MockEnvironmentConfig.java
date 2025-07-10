package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 提供Environment的mock实现
 * 为解决ValidationAutoConfiguration中的NullPointerException
 * 同时提供ConfigurableEnvironment，解决RocketMQ自动配置问题
 */
@TestConfiguration
public class MockEnvironmentConfig {

    /**
     * 提供ConfigurableEnvironment的实现
     * 同时满足Environment接口需求和RocketMQ的ConfigurableEnvironment需求
     */
    @Bean(name = "testEnvironment")
    @ConditionalOnMissingBean(Environment.class)
    public ConfigurableEnvironment testEnvironment() {
        // 使用StandardEnvironment的实例，而不是mock
        StandardEnvironment environment = new StandardEnvironment();
        
        // 为ValidationAutoConfiguration等配置必要的属性
        System.setProperty("spring.validation.is-method-validation-bean-post-processor-enabled", "false");
        System.setProperty("rocketmq.name-server", "8.133.246.113:9876");
        System.setProperty("rocketmq.producer.group", "test-group");
        
        return environment;
    }
} 