package com.bgpay.bgai.config;

import com.bgpay.bgai.service.DynamicRouteService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * DynamicRouteService Mock配置类
 * 提供测试环境使用的DynamicRouteService实现
 */
@TestConfiguration
public class DynamicRouteServiceMockConfig {

    /**
     * 提供DynamicRouteService mock
     */
    @Primary
    @Bean
    public DynamicRouteService dynamicRouteService() {
        return Mockito.mock(DynamicRouteService.class);
    }
} 