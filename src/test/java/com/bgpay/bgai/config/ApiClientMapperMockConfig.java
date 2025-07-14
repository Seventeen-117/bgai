package com.bgpay.bgai.config;

import com.bgpay.bgai.mapper.ApiClientMapper;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * ApiClientMapper Mock配置类
 * 提供测试环境使用的ApiClientMapper实现
 */
@TestConfiguration
public class ApiClientMapperMockConfig {

    /**
     * 提供ApiClientMapper mock
     */
    @Primary
    @Bean
    public ApiClientMapper apiClientMapper() {
        return Mockito.mock(ApiClientMapper.class);
    }
} 