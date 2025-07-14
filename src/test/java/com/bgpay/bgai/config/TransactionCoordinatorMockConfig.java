package com.bgpay.bgai.config;

import com.bgpay.bgai.transaction.TransactionCoordinator;
import com.bgpay.bgai.transaction.TransactionPhase;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 提供TransactionCoordinator的Mock实现，用于测试环境
 */
@Configuration
public class TransactionCoordinatorMockConfig {

    /**
     * 提供一个Mock的TransactionCoordinator bean，替代原始实现
     */
    @Bean
    @Primary
    public TransactionCoordinator transactionCoordinator() {
        TransactionCoordinator mockCoordinator = Mockito.mock(TransactionCoordinator.class);
        
        // 配置基本行为
        Mockito.when(mockCoordinator.prepare(Mockito.anyString()))
               .thenAnswer(invocation -> "chat-" + java.util.UUID.randomUUID().toString());
        
        Mockito.when(mockCoordinator.commit(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
               .thenReturn(true);
        
        Mockito.when(mockCoordinator.hasActiveTransaction(Mockito.anyString()))
               .thenReturn(false);
        
        Mockito.when(mockCoordinator.getCurrentCompletionId(Mockito.anyString()))
               .thenReturn(null);
        
        return mockCoordinator;
    }
} 