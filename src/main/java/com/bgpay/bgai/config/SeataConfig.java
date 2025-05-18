package com.bgpay.bgai.config;

import io.seata.spring.annotation.GlobalTransactionScanner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Seata配置类，用于配置分布式事务
 * 项目采用Seata作为分布式事务处理解决方案
 * 使用AT模式实现两阶段提交协议
 * 使用Saga模式进行事务补偿
 */
@Configuration
public class SeataConfig {

    @Value("${spring.application.name}")
    private String applicationId;

    @Value("${seata.tx-service-group:bgai-tx-group}")
    private String txServiceGroup;

    /**
     * 配置全局事务扫描器
     * @return GlobalTransactionScanner
     */
    @Bean
    public GlobalTransactionScanner globalTransactionScanner() {
        return new GlobalTransactionScanner(applicationId, txServiceGroup);
    }
} 