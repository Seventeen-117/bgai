package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * 禁用RocketMQ相关的自动配置
 * 用于测试环境，避免RocketMQ相关配置与测试配置冲突
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration.class,
    org.apache.rocketmq.spring.autoconfigure.ListenerContainerConfiguration.class,
    org.apache.rocketmq.spring.autoconfigure.RocketMQTransactionConfiguration.class,
    org.apache.rocketmq.spring.autoconfigure.ExtConsumerResetConfiguration.class
})
public class RocketMQAutoConfigurationDisabler {
    // 仅用于禁用自动配置，无需其他内容
} 