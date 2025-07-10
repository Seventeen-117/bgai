package com.bgpay.bgai.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.support.TestPropertySourceUtils;

/**
 * Spring测试配置类
 * 用于初始化测试环境的ApplicationContext
 */
public class SpringTestConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        // 禁用RocketMQ相关功能
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            applicationContext,
            "rocketmq.messageConsumer.enabled=false",
            "rocketmq.producer.enable=false",
            "rocketmq.name-server=8.133.246.113:9876",
            "rocketmq.producer.group=test-group"
        );
        
        // 禁用Seata相关功能
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            applicationContext, 
            "seata.enabled=false",
            "saga.enabled=false",
            "seata.saga.state-machine.auto-register=false"
        );
        
        // 配置标准环境
        ConfigurableEnvironment environment = new StandardEnvironment();
        applicationContext.setEnvironment(environment);
    }
} 