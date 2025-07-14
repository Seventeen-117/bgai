package com.bgpay.bgai.config;

import com.bgpay.bgai.mapper.ChoicesMapper;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * ChoicesMapper Mock配置类
 * 提供测试环境使用的ChoicesMapper实现
 */
@TestConfiguration
public class ChoicesMapperMockConfig {

    /**
     * 提供ChoicesMapper mock
     */
    @Primary
    @Bean
    public ChoicesMapper choicesMapper() {
        return Mockito.mock(ChoicesMapper.class);
    }
} 