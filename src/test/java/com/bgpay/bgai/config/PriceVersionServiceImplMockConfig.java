package com.bgpay.bgai.config;

import com.bgpay.bgai.entity.PriceVersion;
import com.bgpay.bgai.service.PriceVersionService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Mock configuration for PriceVersionServiceImpl
 */
@Configuration
public class PriceVersionServiceImplMockConfig {

    @Bean
    @Primary
    public PriceVersionService priceVersionService() {
        PriceVersionService mockService = Mockito.mock(PriceVersionService.class);
        
        // Mock default behaviors
        Mockito.when(mockService.getCurrentVersion(Mockito.anyString()))
                .thenReturn(1);
        
        return mockService;
    }
} 