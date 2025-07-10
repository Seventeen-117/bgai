package com.bgpay.bgai.config;

import com.bgpay.bgai.service.deepseek.ConfigLoader;
import com.bgpay.bgai.service.deepseek.ConversationHistoryService;
import com.bgpay.bgai.service.deepseek.DeepSeekService;
import com.bgpay.bgai.service.deepseek.DeepSeekServiceImp;
import com.bgpay.bgai.service.deepseek.FileProcessor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.Resource;
import java.lang.reflect.Field;

/**
 * DeepSeek服务相关的mock配置
 * 提供DeepSeekService及其依赖的mock实现
 */
@TestConfiguration
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // 确保在WebClient相关配置之后执行
public class DeepSeekServiceMockConfig {

    /**
     * 提供DeepSeekService的mock实现
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(DeepSeekService.class)
    public DeepSeekService deepSeekService() {
        return Mockito.mock(DeepSeekService.class);
    }
    
    /**
     * 提供DeepSeekServiceImp的mock实现
     * 使用@Primary注解，确保这个mock实现被注入到其他bean中
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(DeepSeekServiceImp.class)
    public DeepSeekServiceImp deepSeekServiceImp(@Autowired WebClient webClient) {
        DeepSeekServiceImp mock = Mockito.mock(DeepSeekServiceImp.class);
        
        // 使用构造的WebClient实例，避免自动注入导致的冲突
        try {
            Field webClientField = DeepSeekServiceImp.class.getDeclaredField("webClient");
            webClientField.setAccessible(true);
            webClientField.set(mock, webClient);
        } catch (Exception e) {
            System.err.println("无法设置DeepSeekServiceImp的webClient字段: " + e.getMessage());
        }
        
        return mock;
    }
    
    /**
     * 提供ConfigLoader的mock实现
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(ConfigLoader.class)
    public ConfigLoader configLoader() {
        return Mockito.mock(ConfigLoader.class);
    }
    
    /**
     * 提供ConversationHistoryService的mock实现
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(ConversationHistoryService.class)
    public ConversationHistoryService conversationHistoryService() {
        return Mockito.mock(ConversationHistoryService.class);
    }
    
    /**
     * 提供FileProcessor的mock实现
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(FileProcessor.class)
    public FileProcessor fileProcessor() {
        return Mockito.mock(FileProcessor.class);
    }
} 