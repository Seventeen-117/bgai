package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 自动配置排除过滤器，用于排除RocketMQ消费者自动配置
 * 通过META-INF/spring.factories自动加载
 */
public class RocketMQConsumerAutoConfigurationExclusionFilter implements AutoConfigurationImportFilter {
    
    private static final Set<String> EXCLUDED_AUTO_CONFIGURATIONS = new HashSet<>(Arrays.asList(
        "org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration",
        "org.apache.rocketmq.spring.autoconfigure.ListenerContainerConfiguration",
        "org.apache.rocketmq.spring.autoconfigure.RocketMQTransactionConfiguration",
        "org.apache.rocketmq.spring.autoconfigure.ExtConsumerResetConfiguration"
    ));
    
    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean[] matches = new boolean[autoConfigurationClasses.length];
        
        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            matches[i] = !EXCLUDED_AUTO_CONFIGURATIONS.contains(autoConfigurationClasses[i]);
        }
        
        return matches;
    }
} 