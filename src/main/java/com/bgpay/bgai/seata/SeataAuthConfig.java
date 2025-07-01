package com.bgpay.bgai.seata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Seata认证配置
 * 用于处理Seata的安全认证配置
 */
@Configuration
@ConditionalOnProperty(prefix = "seata", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SeataAuthConfig {
    private static final Logger logger = LoggerFactory.getLogger(SeataAuthConfig.class);
    
    @Value("${seata.saga.state-machine.auto-register:false}")
    private boolean autoRegister;
    
    @Bean
    public Object logSeataConfig() {
        logger.info("Seata Saga配置加载");
        logger.info("状态机自动注册设置: {}", autoRegister ? "启用" : "禁用");
        
        // 确保状态机自动注册被禁用
        if (autoRegister) {
            logger.warn("检测到状态机自动注册被启用，这可能导致重复注册错误。建议在file.conf中设置saga.state-machine.auto-register=false");
            // 设置系统属性强制禁用
            System.setProperty("seata.saga.state-machine.auto-register", "false");
            logger.info("已强制设置seata.saga.state-machine.auto-register=false");
        } else {
            logger.info("状态机自动注册已禁用，这将防止重复注册错误");
        }
        
        // 返回一个空对象
        return new Object();
    }
} 