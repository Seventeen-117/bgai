package com.bgpay.bgai.config;

import com.bgpay.bgai.filter.ChatRecordWebFilter;
import com.bgpay.bgai.repository.es.ChatRecordRepository;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * ChatRecordWebFilter Mock配置类
 * 提供测试环境使用的ChatRecordWebFilter实现
 */
@TestConfiguration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ChatWebFilterMockConfig {

    /**
     * 提供ChatRecordWebFilter mock
     */
    @Bean
    @Primary
    public ChatRecordWebFilter chatRecordWebFilter(ChatRecordRepository chatRecordRepository) {
        return Mockito.mock(ChatRecordWebFilter.class);
    }
}