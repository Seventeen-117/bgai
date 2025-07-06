package com.bgpay.bgai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import jakarta.annotation.PostConstruct;

/**
 * Seata禁用配置类
 * 专门用于禁用测试环境中的Seata配置，避免"service.vgroupMapping.default_tx_group configuration item is required"错误
 */
@Configuration
@Order(Integer.MIN_VALUE) // 最高优先级，确保在其他配置之前加载
@Profile({"idempotence-test", "test", "webflux-test"})
public class SeataDisableConfiguration {
    private static final Logger log = LoggerFactory.getLogger(SeataDisableConfiguration.class);
    
    public SeataDisableConfiguration() {
        log.info("初始化Seata禁用配置，用于解决测试环境中的Seata连接错误");
        // 明确设置系统属性以禁用Seata
        System.setProperty("seata.enabled", "false");
        System.setProperty("spring.cloud.alibaba.seata.enabled", "false");
        System.setProperty("seata.enable-auto-data-source-proxy", "false");
        System.setProperty("seata.service.disable-global-transaction", "true");
    }
    
    @PostConstruct
    public void init() {
        log.info("PostConstruct: 禁用Seata配置加载完成，确保Seata不会初始化");
        System.setProperty("seata.enabled", "false");
        System.setProperty("seata.registry.enabled", "false");
        System.setProperty("seata.config.enabled", "false");
        
        // 尝试设置JVM安全属性禁用Seata的全局事务
        try {
            System.setSecurityManager(null);
        } catch (Exception e) {
            log.warn("无法设置安全管理器: {}", e.getMessage());
        }
    }
    
    /**
     * 提供一个空的GlobalTransactionScanner的替代Bean
     * 以避免自动配置尝试初始化真实的Seata客户端
     */
    @Bean
    @Primary
    @Profile({"idempotence-test", "test", "webflux-test"})
    public Object mockSeataGlobalTransactionScanner() {
        log.info("提供Mock的Seata GlobalTransactionScanner");
        // 返回一个简单对象代替真实的Seata GlobalTransactionScanner
        return new Object();
    }
    
    /**
     * 提供一个空的TransactionalTemplate的替代Bean
     */
    @Bean
    @Primary
    @Profile({"idempotence-test", "test", "webflux-test"})
    public Object mockSeataTransactionalTemplate() {
        log.info("提供Mock的Seata TransactionalTemplate");
        return new Object();
    }
    
    /**
     * 提供一个空的TmRpcClient的替代Bean
     */
    @Bean
    @Primary
    @Profile({"idempotence-test", "test", "webflux-test"})
    public Object mockSeataTmRpcClient() {
        log.info("提供Mock的Seata TmRpcClient");
        return new Object();
    }
} 