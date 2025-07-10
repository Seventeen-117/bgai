package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * 禁用主应用的DataSourceConfig配置
 * 并导入测试环境专用的DataSourceMockConfig
 */
@Configuration
@Import(DataSourceMockConfig.class)
@EnableAutoConfiguration(exclude = {
    com.bgpay.bgai.datasource.DataSourceConfig.class
})
public class DataSourceAutoConfigurationDisabler {
    // 仅用于禁用主应用的DataSourceConfig
    // 并导入测试专用的DataSourceMockConfig
} 