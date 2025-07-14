package com.bgpay.bgai.config;

import com.bgpay.bgai.mapper.ApiConfigMapper;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 提供ApiConfigMapper的Mock实现，用于测试环境
 */
@Configuration
public class ApiConfigMapperMockConfig {

    /**
     * 提供一个Mock的ApiConfigMapper bean，替代原始实现
     */
    @Bean
    @Primary
    public ApiConfigMapper apiConfigMapper() {
        return Mockito.mock(ApiConfigMapper.class);
    }
} 