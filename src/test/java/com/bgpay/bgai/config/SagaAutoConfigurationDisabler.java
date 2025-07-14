package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * 禁用Seata Saga自动配置
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    com.bgpay.bgai.seata.SagaStateMachineConfig.class,
    com.bgpay.bgai.seata.SeataSagaConfig.class,
    com.bgpay.bgai.seata.SeataAuthConfig.class
})
public class SagaAutoConfigurationDisabler {
    // 仅用于禁用Seata Saga相关的自动配置
} 