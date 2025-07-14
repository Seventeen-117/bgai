package com.bgpay.bgai.config;

import io.seata.saga.engine.StateMachineEngine;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 提供Saga状态机相关组件的Mock实现，用于测试环境
 */
@Configuration
public class SagaStateMachineMockConfig {

    /**
     * 提供一个Mock的StateMachineEngine bean，替代原始实现
     */
    @Bean
    @Primary
    public StateMachineEngine stateMachineEngine() {
        StateMachineEngine mockEngine = Mockito.mock(StateMachineEngine.class);
        return mockEngine;
    }
} 