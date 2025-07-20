package com.bgpay.bgai.config;

import com.bgpay.bgai.service.PriceCacheService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class PriceCacheServiceMockConfig {
    @Bean
    @Primary
    public PriceCacheService priceCacheService() {
        return Mockito.mock(PriceCacheService.class);
    }
} 