package com.bgpay.bgai.config;

import com.bgpay.bgai.controller.UsageStatisticsController;
import com.bgpay.bgai.service.UsageRecordService;
import com.bgpay.bgai.service.UsageInfoService;
import com.bgpay.bgai.service.BillingService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Mock configuration for UsageStatisticsController and its dependencies
 */
@Configuration
public class UsageStatisticsControllerMockConfig {

    @Bean
    @Primary
    public UsageStatisticsController usageStatisticsController() {
        UsageRecordService mockUsageRecordService = Mockito.mock(UsageRecordService.class);
        UsageInfoService mockUsageInfoService = Mockito.mock(UsageInfoService.class);
        BillingService mockBillingService = Mockito.mock(BillingService.class);
        
        // Create a spy to allow the controller to function but override problematic methods
        return new UsageStatisticsController(mockUsageRecordService, mockUsageInfoService, mockBillingService);
    }
} 