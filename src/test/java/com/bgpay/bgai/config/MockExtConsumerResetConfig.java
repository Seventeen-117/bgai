package com.bgpay.bgai.config;

import org.apache.rocketmq.spring.autoconfigure.ExtConsumerResetConfiguration;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 模拟RocketMQ的ExtConsumerResetConfiguration配置类
 * 解决RocketMQ自动配置导致的依赖问题
 */
@Configuration
@AutoConfigureBefore(org.apache.rocketmq.spring.autoconfigure.ExtConsumerResetConfiguration.class)
public class MockExtConsumerResetConfig {

    /**
     * 提供模拟的ExtConsumerResetConfiguration
     * 避免RocketMQ自动配置创建真实实例
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    public ExtConsumerResetConfiguration extConsumerResetConfiguration() {
        return Mockito.mock(ExtConsumerResetConfiguration.class);
    }
} 