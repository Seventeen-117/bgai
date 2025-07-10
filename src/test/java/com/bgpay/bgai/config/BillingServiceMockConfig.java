package com.bgpay.bgai.config;

import com.bgpay.bgai.entity.UsageRecord;
import com.bgpay.bgai.service.BillingService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Mock configuration for BillingService in test environment
 */
@Configuration
public class BillingServiceMockConfig {

    @Bean
    @Primary
    public BillingService billingService() {
        BillingService mockService = Mockito.mock(BillingService.class);
        
        // Configure processBatch to do nothing
        Mockito.doNothing().when(mockService).processBatch(Mockito.anyList(), Mockito.anyString());
        
        return mockService;
    }
} 