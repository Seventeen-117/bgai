package com.bgpay.bgai.config;

import com.bgpay.bgai.entity.PriceConfig;
import com.bgpay.bgai.entity.PriceQuery;
import com.bgpay.bgai.service.PriceConfigService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;

/**
 * Mock configuration for PriceConfigService in test environment
 */
@Configuration
public class PriceConfigServiceMockConfig {

    @Bean
    @Primary
    public PriceConfigService priceConfigService() {
        PriceConfigService mockService = Mockito.mock(PriceConfigService.class);
        
        // Configure findValidPriceConfig to return a default PriceConfig
        PriceConfig defaultConfig = new PriceConfig();
        defaultConfig.setPrice(BigDecimal.valueOf(0.01));
        
        Mockito.when(mockService.findValidPriceConfig(Mockito.any(PriceQuery.class)))
               .thenReturn(defaultConfig);
        
        // Configure getPrice to return a default price
        Mockito.when(mockService.getPrice(Mockito.any(PriceQuery.class)))
               .thenReturn(BigDecimal.valueOf(0.01));
        
        // Configure getCurrentPriceVersion to return 1
        Mockito.when(mockService.getCurrentPriceVersion())
               .thenReturn(1);
        
        return mockService;
    }
} 