package com.bgpay.bgai.config;

import com.bgpay.bgai.entity.ChatCompletions;
import com.bgpay.bgai.service.ChatCompletionsService;
import org.mockito.Mockito;
import static org.mockito.Mockito.doNothing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 提供ChatCompletionsService的Mock实现，用于测试环境
 */
@Configuration
public class ChatCompletionsServiceMockConfig {

    /**
     * 提供一个Mock的ChatCompletionsService bean，替代原始实现
     */
    @Bean
    @Primary
    public ChatCompletionsService chatCompletionsService() {
        ChatCompletionsService mockService = Mockito.mock(ChatCompletionsService.class);
        
        // 配置基本行为 - 使用doNothing()处理void方法
        doNothing().when(mockService).insertChatCompletions(Mockito.any(ChatCompletions.class));
        
        return mockService;
    }
} 