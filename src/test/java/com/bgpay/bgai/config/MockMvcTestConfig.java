package com.bgpay.bgai.config;

import com.bgpay.bgai.controller.AuthController;
import com.bgpay.bgai.filter.ApiKeyAuthenticationFilter;
import com.bgpay.bgai.filter.AuthenticationFilter;
import com.bgpay.bgai.service.ApiConfigService;
import com.bgpay.bgai.service.ApiKeyService;
import com.bgpay.bgai.service.BGAIService;
import com.bgpay.bgai.service.TransactionLogService;
import com.bgpay.bgai.service.UserService;
import com.bgpay.bgai.service.deepseek.DeepSeekService;
import com.bgpay.bgai.service.deepseek.DeepSeekServiceImp;
import com.bgpay.bgai.service.deepseek.FileProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * MockMvc测试配置类
 * 提供MockMvc测试所需的bean
 */
@TestConfiguration
public class MockMvcTestConfig {

    /**
     * 提供ObjectMapper bean
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
    
    /**
     * 提供Environment mock
     */
    @Bean
    public Environment environment() {
        return Mockito.mock(Environment.class);
    }
    
    /**
     * 提供AuthController bean
     */
    @Bean
    public AuthController authController(UserService userService, Environment environment) {
        AuthController controller = new AuthController();
        return controller;
    }
    
    /**
     * 提供DeepSeekService mock
     */
    @Bean
    public DeepSeekService deepSeekService() {
        return Mockito.mock(DeepSeekService.class);
    }
    
    /**
     * 提供DeepSeekServiceImp mock
     */
    @Bean
    public DeepSeekServiceImp deepSeekServiceImp() {
        return Mockito.mock(DeepSeekServiceImp.class);
    }
    
    /**
     * 提供FileProcessor mock
     */
    @Bean
    public FileProcessor fileProcessor() {
        return Mockito.mock(FileProcessor.class);
    }
    
    /**
     * 提供TransactionLogService mock
     */
    @Bean
    public TransactionLogService transactionLogService() {
        return Mockito.mock(TransactionLogService.class);
    }
    
    /**
     * 提供WebClient mock
     */
    @Bean
    public WebClient webClient() {
        return Mockito.mock(WebClient.class);
    }
    
    /**
     * 提供WebClient.Builder mock
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        WebClient.Builder mockBuilder = Mockito.mock(WebClient.Builder.class);
        WebClient mockWebClient = Mockito.mock(WebClient.class);
        
        // 配置mock行为
        Mockito.when(mockBuilder.build()).thenReturn(mockWebClient);
        Mockito.when(mockBuilder.baseUrl(Mockito.anyString())).thenReturn(mockBuilder);
        Mockito.when(mockBuilder.filter(Mockito.any())).thenReturn(mockBuilder);
        
        return mockBuilder;
    }
    
    /**
     * 提供ApiKeyAuthenticationFilter mock
     */
    @Bean
    public ApiKeyAuthenticationFilter apiKeyAuthenticationFilter() {
        return Mockito.mock(ApiKeyAuthenticationFilter.class);
    }
    
    /**
     * 提供AuthenticationFilter mock
     */
    @Bean
    public AuthenticationFilter authenticationFilter() {
        return Mockito.mock(AuthenticationFilter.class);
    }
} 