package com.bgpay.bgai.config;

import com.bgpay.bgai.controller.SystemConfigController;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * SystemConfigController Mock配置类
 * 提供测试环境使用的SystemConfigController实现
 */
@TestConfiguration
public class SystemConfigControllerMockConfig {

    /**
     * 提供SystemConfigController mock
     */
    @Primary
    @Bean
    public SystemConfigController systemConfigController() {
        return Mockito.mock(SystemConfigController.class);
    }
} 