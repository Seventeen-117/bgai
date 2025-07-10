package com.bgpay.bgai.config;

import com.bgpay.bgai.service.BGAIService;
import com.bgpay.bgai.service.impl.BGAIServiceImpl;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

/**
 * BGAIService Mock配置类
 * 提供测试环境使用的BGAIService实现
 */
@TestConfiguration
@Order(-2147483648) // Integer.MIN_VALUE，确保最高优先级
public class BGAIServiceMockConfig {

    /**
     * 提供BGAIServiceImpl mock实例
     * 注意: 这里必须最先定义BGAIServiceImpl，因为其他bean依赖它
     * 使用@Primary确保它被优先注入
     */
    @Bean
    @Primary
    public BGAIServiceImpl bgaiServiceImpl() {
        return Mockito.mock(BGAIServiceImpl.class);
    }
    
    /**
     * 提供BGAIService mock
     * 这个实际上是没有那么紧急的，因为BGAIServiceImpl已经实现了BGAIService接口
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(BGAIService.class)
    public BGAIService bgaiService() {
        // 重要：返回同一个实例以避免多个mock
        return bgaiServiceImpl();
    }
} 