package com.bgpay.bgai.config;

import com.bgpay.bgai.service.TransactionLogService;
import com.bgpay.bgai.service.impl.TransactionLogServiceImpl;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * TransactionLogService服务相关的mock配置
 * 提供TransactionLogService的mock实现
 */
@TestConfiguration
public class TransactionLogServiceMockConfig {

    /**
     * 提供TransactionLogService的mock实现
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(TransactionLogService.class)
    public TransactionLogService transactionLogService() {
        return Mockito.mock(TransactionLogService.class);
    }
    
    /**
     * 提供TransactionLogServiceImpl的mock实现
     * 不使用@Primary注解，避免与transactionLogService冲突
     * transactionLogServiceImpl名称与TransactionLogServiceImpl类创建的默认bean名称相同
     */
    @Bean
    @ConditionalOnMissingBean(TransactionLogServiceImpl.class)
    public TransactionLogServiceImpl transactionLogServiceImpl() {
        return Mockito.mock(TransactionLogServiceImpl.class);
    }
} 