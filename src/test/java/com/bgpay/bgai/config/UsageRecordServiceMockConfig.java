package com.bgpay.bgai.config;

import com.bgpay.bgai.entity.UsageRecord;
import com.bgpay.bgai.service.UsageRecordService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Mock configuration for UsageRecordService in test environment
 */
@Configuration
public class UsageRecordServiceMockConfig {

    @Bean
    @Primary
    public UsageRecordService usageRecordService() {
        UsageRecordService mockService = Mockito.mock(UsageRecordService.class);
        
        // Configure basic behaviors for commonly used methods
        Mockito.when(mockService.save(Mockito.any(UsageRecord.class))).thenReturn(true);
        
        return mockService;
    }
} 