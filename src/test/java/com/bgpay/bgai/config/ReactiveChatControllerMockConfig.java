package com.bgpay.bgai.config;

import com.bgpay.bgai.controller.ReactiveChatController;
import com.bgpay.bgai.response.ChatResponse;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 提供ReactiveChatController的Mock实现，用于测试环境
 */
@Configuration
public class ReactiveChatControllerMockConfig {

    /**
     * 提供一个Mock的ReactiveChatController bean，替代原始实现
     */
    @Bean
    @Primary
    public ReactiveChatController reactiveChatController() {
        ReactiveChatController mockController = Mockito.mock(ReactiveChatController.class);
        
        // 配置基本行为
        ChatResponse mockResponse = new ChatResponse();
        mockResponse.setSuccess(true);
        mockResponse.setContent("This is a mock response from ReactiveChatController");
        
        Mockito.when(mockController.handleChatRequest(
                Mockito.any(FilePart.class), Mockito.anyString(), 
                Mockito.anyString(), Mockito.anyString(), 
                Mockito.anyString(), Mockito.anyString(), 
                Mockito.any(ServerWebExchange.class)
        )).thenReturn(Mono.just(ResponseEntity.ok(mockResponse)));
        
        return mockController;
    }
} 