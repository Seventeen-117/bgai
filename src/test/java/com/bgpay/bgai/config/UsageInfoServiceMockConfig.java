package com.bgpay.bgai.config;

import com.bgpay.bgai.service.UsageInfoService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 提供UsageInfoService的Mock实现，用于测试环境
 */
@Configuration
public class UsageInfoServiceMockConfig {

    /**
     * 提供一个Mock的UsageInfoService bean，替代原始实现
     */
    @Bean
    @Primary
    public UsageInfoService usageInfoService() {
        return Mockito.mock(UsageInfoService.class);
    }
} 