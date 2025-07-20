package com.bgpay.bgai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 禁用主应用的DataSourceConfig配置
 * 并导入测试环境专用的DataSourceMockConfig
 */
@Configuration
@Import(DataSourceMockConfig.class)
public class DataSourceAutoConfigurationDisabler {
    // 仅用于禁用主应用的DataSourceConfig
    // 并导入测试专用的DataSourceMockConfig
} 