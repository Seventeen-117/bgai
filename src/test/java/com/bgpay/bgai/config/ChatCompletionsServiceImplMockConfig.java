package com.bgpay.bgai.config;

import com.bgpay.bgai.service.ChatCompletionsService;
import com.bgpay.bgai.service.impl.ChatCompletionsServiceImpl;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * ChatCompletionsServiceImpl Mock配置类
 * 提供测试环境使用的ChatCompletionsServiceImpl实现
 */
@TestConfiguration
public class ChatCompletionsServiceImplMockConfig {

    /**
     * 提供ChatCompletionsService mock
     */
    @Primary
    @Bean
    public ChatCompletionsService chatCompletionsService() {
        return Mockito.mock(ChatCompletionsService.class);
    }
    
    /**
     * 提供ChatCompletionsServiceImpl mock
     */
    @Bean
    public ChatCompletionsServiceImpl chatCompletionsServiceImpl() {
        return Mockito.mock(ChatCompletionsServiceImpl.class);
    }
} 