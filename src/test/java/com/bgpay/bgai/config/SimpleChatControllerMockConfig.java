package com.bgpay.bgai.config;

import com.bgpay.bgai.controller.SimpleChatController;
import com.bgpay.bgai.response.SimpleChatResponse;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 提供SimpleChatController的Mock实现，用于测试环境
 */
@Configuration
public class SimpleChatControllerMockConfig {

    /**
     * 提供一个Mock的SimpleChatController bean，替代原始实现
     */
    @Bean
    @Primary
    public SimpleChatController simpleChatController() {
        SimpleChatController mockController = Mockito.mock(SimpleChatController.class);
        
        // 配置基本行为
        Mockito.when(mockController.handleSimpleChatRequest(
                Mockito.any(), Mockito.anyString(), Mockito.anyString(), 
                Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean(), 
                Mockito.any(ServerWebExchange.class)
        )).thenReturn(
            Mono.just(ResponseEntity.ok(
                SimpleChatResponse.builder()
                    .content("This is a mock response from SimpleChatController")
                    .build()
            ))
        );
        
        return mockController;
    }
} 