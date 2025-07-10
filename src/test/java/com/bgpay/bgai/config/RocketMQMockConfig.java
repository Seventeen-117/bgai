package com.bgpay.bgai.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.mockito.Mockito;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;

/**
 * RocketMQ的Mock配置
 * 提供RocketMQ相关的组件Mock实例，避免测试时连接真实的RocketMQ服务
 */
@TestConfiguration
public class RocketMQMockConfig {
    
    /**
     * 提供一个Mock的DefaultMQProducer
     */
    @Bean
    @Primary
    public DefaultMQProducer defaultMQProducer() {
        return Mockito.mock(DefaultMQProducer.class);
    }
    
    /**
     * 提供一个Mock的RocketMQTemplate
     */
    @Bean
    @Primary
    public RocketMQTemplate rocketMQTemplate() {
        return Mockito.mock(RocketMQTemplate.class);
    }
} 