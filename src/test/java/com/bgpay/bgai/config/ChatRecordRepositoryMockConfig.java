package com.bgpay.bgai.config;

import com.bgpay.bgai.repository.es.ChatRecordRepository;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * ChatRecordRepository Mock配置类
 * 提供测试环境使用的ChatRecordRepository实现
 */
@TestConfiguration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ChatRecordRepositoryMockConfig {

    /**
     * 提供ChatRecordRepository mock
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(ChatRecordRepository.class)
    public ChatRecordRepository chatRecordRepository() {
        return Mockito.mock(ChatRecordRepository.class);
    }
}