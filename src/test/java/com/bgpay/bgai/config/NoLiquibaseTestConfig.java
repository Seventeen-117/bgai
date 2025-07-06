package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * 测试配置类，用于禁用Liquibase和数据库相关的自动配置
 * 避免测试中加载不必要的数据库配置导致错误
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    LiquibaseAutoConfiguration.class
})
public class NoLiquibaseTestConfig {
    // 不需要额外的配置，仅作为标记类使用
} 