package com.bgpay.bgai.config;

import com.bgpay.bgai.service.impl.FallbackService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class FallbackServiceMockConfig {
    @Bean
    @Primary
    public FallbackService fallbackService() {
        return Mockito.mock(FallbackService.class);
    }
} 