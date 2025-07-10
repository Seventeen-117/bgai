package com.bgpay.bgai.config;

import com.bgpay.bgai.service.ApiKeyService;
import com.bgpay.bgai.service.impl.ApiKeyServiceImpl;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * ApiKeyService Mock配置类
 * 提供测试环境使用的ApiKeyService实现
 */
@TestConfiguration
public class ApiKeyServiceMockConfig {

    /**
     * 提供ApiKeyService mock
     */
    @Primary
    @Bean
    @ConditionalOnMissingBean(ApiKeyService.class)
    public ApiKeyService apiKeyService() {
        return Mockito.mock(ApiKeyService.class);
    }
    
    /**
     * 提供ApiKeyServiceImpl mock
     * 用于解决ApiKeyService有多个实现的问题
     * apiKeyServiceImpl名称与ApiKeyServiceImpl类创建的默认bean名称相同
     */
    @Bean
    @ConditionalOnMissingBean(ApiKeyServiceImpl.class)
    public ApiKeyServiceImpl apiKeyServiceImpl() {
        return Mockito.mock(ApiKeyServiceImpl.class);
    }
} 