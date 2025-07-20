package com.bgpay.bgai.config;

import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class RocketMQTemplateMockConfig {
    @Bean
    @Primary
    public RocketMQTemplate rocketMQTemplate() {
        RocketMQTemplate mockTemplate = Mockito.mock(RocketMQTemplate.class);
        TransactionMQProducer mockProducer = Mockito.mock(TransactionMQProducer.class);
        Mockito.when(mockTemplate.getProducer()).thenReturn(mockProducer);
        return mockTemplate;
    }
} 