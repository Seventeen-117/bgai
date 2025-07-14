package com.bgpay.bgai.config;

import com.bgpay.bgai.service.ChoicesService;
import com.bgpay.bgai.service.impl.ChoicesServiceImpl;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * ChoicesServiceImpl Mock配置类
 * 提供测试环境使用的ChoicesServiceImpl实现
 */
@TestConfiguration
public class ChoicesServiceImplMockConfig {

    /**
     * 提供ChoicesService mock
     */
    @Primary
    @Bean
    public ChoicesService choicesService() {
        return Mockito.mock(ChoicesService.class);
    }
    
    /**
     * 提供ChoicesServiceImpl mock
     */
    @Bean
    public ChoicesServiceImpl choicesServiceImpl() {
        return Mockito.mock(ChoicesServiceImpl.class);
    }
} 