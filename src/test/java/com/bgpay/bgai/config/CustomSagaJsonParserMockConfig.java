package com.bgpay.bgai.config;

import com.bgpay.bgai.saga.CustomSagaJsonParser;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 提供CustomSagaJsonParser的Mock实现，用于测试环境
 */
@Configuration
public class CustomSagaJsonParserMockConfig {

    /**
     * 提供一个Mock的CustomSagaJsonParser bean，替代原始实现
     */
    @Bean
    @Primary
    public CustomSagaJsonParser customSagaJsonParser() {
        CustomSagaJsonParser mockParser = Mockito.mock(CustomSagaJsonParser.class);
        
        // 配置基本行为
        Mockito.when(mockParser.getJsonParserType()).thenReturn("Mock JSON Parser");
        Mockito.when(mockParser.getParsingResultSummary()).thenReturn("Mock parsing results");
        Mockito.when(mockParser.parseStateMachineJson(Mockito.anyString())).thenReturn(true);
        
        return mockParser;
    }
} 