package com.bgpay.bgai.config;

import com.bgpay.bgai.mapper.TransactionLogMapper;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 提供TransactionLogMapper的Mock实现，用于测试环境
 */
@Configuration
public class TransactionLogMapperMockConfig {

    /**
     * 提供一个Mock的TransactionLogMapper bean，替代原始实现
     */
    @Bean
    @Primary
    public TransactionLogMapper transactionLogMapper() {
        return Mockito.mock(TransactionLogMapper.class);
    }
} 