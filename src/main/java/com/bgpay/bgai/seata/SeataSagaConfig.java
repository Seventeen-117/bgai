package com.bgpay.bgai.seata;

import io.seata.spring.boot.autoconfigure.properties.SeataProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Seata Saga配置
 * 用于自定义Seata Saga的行为，特别是避免状态机重复注册
 */
@Configuration
@ConditionalOnProperty(prefix = "saga", name = "enabled", havingValue = "true", matchIfMissing = true)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SeataSagaConfig {

    @Autowired(required = false)
    private SeataProperties seataProperties;

    /**
     * 自定义DbStateMachineConfig，覆盖默认配置
     * 主要是禁用自动注册功能，避免重复键异常
     * 
     * @return 定制的DbStateMachineConfig
     */
    @Bean(name = "dbStateMachineConfig")
    @Primary
    @ConditionalOnMissingBean(name = "dbStateMachineConfig")
    @ConditionalOnProperty(prefix = "saga", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Object customDbStateMachineConfig() {
        // 只是返回一个空对象，用于覆盖原配置
        // 实际配置通过file.conf和SagaStateMachineConfig处理
        // 这样做可以阻止自动配置类创建默认的DbStateMachineConfig
        return new Object();
    }
} 