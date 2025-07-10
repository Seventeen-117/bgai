package com.bgpay.bgai.config;

import com.bgpay.bgai.service.ApiConfigService;
import com.bgpay.bgai.service.TransactionLogService;
import com.bgpay.bgai.service.deepseek.DeepSeekService;
import com.bgpay.bgai.service.deepseek.FileProcessor;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * EnhancedChatController所需的mock配置
 * 提供EnhancedChatController所需的所有依赖
 */
@TestConfiguration
public class EnhancedChatControllerMockConfig {

    /**
     * 提供FileProcessor的mock实现
     */
    @Bean
    @Primary
    public FileProcessor fileProcessor() {
        return Mockito.mock(FileProcessor.class);
    }
    
    /**
     * 提供DeepSeekService的mock实现
     */
    @Bean
    public DeepSeekService deepSeekService() {
        return Mockito.mock(DeepSeekService.class);
    }
    
    /**
     * 提供TransactionLogService的mock实现
     */
    @Bean
    @Primary
    public TransactionLogService transactionLogService() {
        return Mockito.mock(TransactionLogService.class);
    }
} 