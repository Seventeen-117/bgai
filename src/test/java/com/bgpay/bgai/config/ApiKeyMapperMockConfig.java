package com.bgpay.bgai.config;

import com.bgpay.bgai.mapper.ApiKeyMapper;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 提供ApiKeyMapper的Mock实现，用于测试环境
 */
@Configuration
public class ApiKeyMapperMockConfig {

    /**
     * 提供一个Mock的ApiKeyMapper bean，替代原始实现
     */
    @Bean
    @Primary
    public ApiKeyMapper apiKeyMapper() {
        return Mockito.mock(ApiKeyMapper.class);
    }
} 