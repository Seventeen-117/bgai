package com.bgpay.bgai.config;

import com.bgpay.bgai.model.es.ChatRecord;
import com.bgpay.bgai.repository.es.ChatRecordRepository;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * 统一Mock仓库配置类
 * 集中提供各种仓库的Mock实现，避免多个配置类创建相同Bean导致冲突
 */
@Configuration
public class MockRepositoryConfig {
    private static final Logger log = LoggerFactory.getLogger(MockRepositoryConfig.class);
    
    /**
     * 提供统一的ChatRecordRepository Mock实现
     * 适用于响应式和非响应式环境
     */
    @Bean
    @Primary
    public ChatRecordRepository mockChatRecordRepository() {
        log.info("创建统一模拟ChatRecordRepository");
        ChatRecordRepository mockRepository = Mockito.mock(ChatRecordRepository.class);
        
        // 配置通用mock行为 - 处理响应式和非响应式环境
        Mockito.when(mockRepository.save(Mockito.any(ChatRecord.class)))
               .thenAnswer(invocation -> {
                   // 获取保存的对象
                   ChatRecord record = invocation.getArgument(0);
                   // 如果是响应式环境，返回Mono.just(record)，否则直接返回record
                   if (record.getClass().getInterfaces().length > 0 && 
                       record.getClass().getInterfaces()[0].getName().contains("reactor")) {
                       return Mono.just(record);
                   } else {
                       return record;
                   }
               });
                   
        return mockRepository;
    }
} 