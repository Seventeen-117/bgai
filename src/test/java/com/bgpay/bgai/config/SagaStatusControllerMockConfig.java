package com.bgpay.bgai.config;

import com.bgpay.bgai.controller.SagaStatusController;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 提供SagaStatusController的Mock实现，用于测试环境
 */
@Configuration
public class SagaStatusControllerMockConfig {

    /**
     * 提供一个Mock的SagaStatusController bean，替代原始实现
     */
    @Bean
    @Primary
    public SagaStatusController sagaStatusController() {
        SagaStatusController mockController = Mockito.mock(SagaStatusController.class);
        return mockController;
    }
} 