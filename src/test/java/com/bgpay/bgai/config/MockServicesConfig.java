package com.bgpay.bgai.config;

import com.bgpay.bgai.entity.ApiKey;
import com.bgpay.bgai.entity.ApiKeyInfo;
import com.bgpay.bgai.service.ApiKeyService;
import com.bgpay.bgai.service.UserService;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Mock服务配置类
 * 用于在测试环境中提供各种服务的Mock实现
 */
@Configuration
public class MockServicesConfig {
    private static final Logger log = LoggerFactory.getLogger(MockServicesConfig.class);

    @Bean
    @Primary
    public ApiKeyService apiKeyService() {
        log.info("创建Mock ApiKeyService");
        
        ApiKeyService mockService = Mockito.mock(ApiKeyService.class);
        
        // 模拟生成API Key的行为
        Mockito.when(mockService.generateApiKey(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenAnswer(invocation -> {
                String clientId = invocation.getArgument(0);
                String clientName = invocation.getArgument(1);
                String description = invocation.getArgument(2);
                
                return ApiKeyInfo.builder()
                    .apiKey(UUID.randomUUID().toString())
                    .clientId(clientId)
                    .clientName(clientName != null ? clientName : "Test Client")
                    .description(description)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusYears(1))
                    .active(true)
                    .build();
            });
        
        // 模拟校验API Key状态的行为
        Mockito.when(mockService.validateApiKeyStatus(Mockito.anyString()))
            .thenReturn(new ApiKeyService.ApiKeyValidationResult(
                ApiKeyService.ApiKeyStatus.VALID,
                LocalDateTime.now().plusYears(1),
                null,
                "default-client"
            ));
        
        // 模拟获取所有API Key的行为
        Mockito.when(mockService.getAllApiKeys())
            .thenReturn(Collections.emptyList());
        
        return mockService;
    }

    @Bean
    @Primary
    public UserService userService() {
        log.info("创建Mock UserService");
        return Mockito.mock(UserService.class);
    }
} 