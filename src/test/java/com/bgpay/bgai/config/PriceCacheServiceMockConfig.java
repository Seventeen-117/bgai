package com.bgpay.bgai.config;

import com.bgpay.bgai.entity.PriceConfig;
import com.bgpay.bgai.entity.PriceQuery;
import com.bgpay.bgai.service.PriceCacheService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Mock configuration for PriceCacheService in test environment
 */
@Configuration
public class PriceCacheServiceMockConfig {

    @Bean
    @Primary
    public PriceCacheService priceCacheService() {
        PriceCacheService mockService = Mockito.mock(PriceCacheService.class);
        
        // Configure behavior for getPriceConfig
        Mockito.when(mockService.getPriceConfig(Mockito.any(PriceQuery.class))).thenReturn(new PriceConfig());
        
        // Configure behavior for other methods
        Mockito.doNothing().when(mockService).refreshCacheByModel(Mockito.anyString());
        Mockito.doNothing().when(mockService).clearPriceConfigCache();
        
        return mockService;
    }
} 