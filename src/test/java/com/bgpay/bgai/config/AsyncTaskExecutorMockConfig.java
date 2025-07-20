package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.concurrent.Executor;

@TestConfiguration
public class AsyncTaskExecutorMockConfig {
    @Bean
    @Primary
    @Qualifier("asyncTaskExcutor")
    public Executor asyncTaskExcutor() {
        return Mockito.mock(Executor.class);
    }
} 