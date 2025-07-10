package com.bgpay.bgai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * Seata禁用配置
 * 用于在测试环境中禁用Seata相关的自动配置
 * 通过META-INF/spring.factories和META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 * 引入此配置，以确保测试环境不会加载Seata相关组件
 */
@Configuration
public class SeataDisableConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(SeataDisableConfiguration.class);
    
    public SeataDisableConfiguration() {
        // 设置系统属性，禁用Seata
        System.setProperty("seata.enabled", "false");
        System.setProperty("saga.enabled", "false");
        System.setProperty("seata.saga.state-machine.auto-register", "false");
        
        logger.info("SeataDisableConfiguration已加载，Seata在测试环境中被禁用");
        logger.info("设置系统属性: seata.enabled=false, saga.enabled=false");
    }
} 