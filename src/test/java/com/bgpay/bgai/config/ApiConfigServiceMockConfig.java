package com.bgpay.bgai.config;

import com.bgpay.bgai.service.ApiConfigService;
import com.bgpay.bgai.service.impl.ApiConfigServiceImpl;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * ApiConfigService Mock配置类
 * 提供测试环境使用的ApiConfigService实现
 */
@TestConfiguration
public class ApiConfigServiceMockConfig {

    /**
     * 提供ApiConfigService mock
     */
    @Primary
    @Bean
    @ConditionalOnMissingBean(ApiConfigService.class)
    public ApiConfigService apiConfigService() {
        return Mockito.mock(ApiConfigService.class);
    }
    
    /**
     * 提供ApiConfigServiceImpl mock
     * 用于解决ApiConfigService有多个实现的问题
     * apiConfigServiceImpl名称与ApiConfigServiceImpl类创建的默认bean名称相同
     */
    @Bean
    @ConditionalOnMissingBean(ApiConfigServiceImpl.class)
    public ApiConfigServiceImpl apiConfigServiceImpl() {
        return Mockito.mock(ApiConfigServiceImpl.class);
    }
} 