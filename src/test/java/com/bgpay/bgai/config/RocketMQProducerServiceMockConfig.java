package com.bgpay.bgai.config;

import com.bgpay.bgai.service.mq.RocketMQProducerService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class RocketMQProducerServiceMockConfig {
    @Bean
    @Primary
    public RocketMQProducerService rocketMQProducerService() {
        return Mockito.mock(RocketMQProducerService.class);
    }
} 