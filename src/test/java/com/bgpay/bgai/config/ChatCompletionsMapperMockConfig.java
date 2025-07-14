package com.bgpay.bgai.config;

import com.bgpay.bgai.mapper.ChatCompletionsMapper;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * ChatCompletionsMapper Mock配置类
 * 提供测试环境使用的ChatCompletionsMapper实现
 */
@TestConfiguration
public class ChatCompletionsMapperMockConfig {

    /**
     * 提供ChatCompletionsMapper mock
     */
    @Primary
    @Bean
    public ChatCompletionsMapper chatCompletionsMapper() {
        return Mockito.mock(ChatCompletionsMapper.class);
    }
} 