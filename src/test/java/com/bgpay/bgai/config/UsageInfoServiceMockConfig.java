package com.bgpay.bgai.config;

import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.bgpay.bgai.entity.UsageInfo;
import com.bgpay.bgai.service.UsageInfoService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;

/**
 * Mock configuration for UsageInfoService in test environment
 */
@Configuration
public class UsageInfoServiceMockConfig {

    @Bean
    @Primary
    public UsageInfoService usageInfoService() {
        UsageInfoService mockService = Mockito.mock(UsageInfoService.class);
        
        // Configure processUsageInfo to return true
        Mockito.when(mockService.processUsageInfo(
                Mockito.any(UsageCalculationDTO.class), 
                Mockito.anyString())).thenReturn(true);
        
        // Configure a basic method to return a default UsageInfo
        UsageInfo defaultInfo = new UsageInfo();
        defaultInfo.setUserId("test-user");
        defaultInfo.setCreatedAt(LocalDateTime.now());
        
        return mockService;
    }
} 