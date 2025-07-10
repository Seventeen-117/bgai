package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * 禁用WebMvc自动配置
 * 用于测试环境，避免自动配置与测试配置冲突
 */
@Configuration
@EnableAutoConfiguration(exclude = {WebMvcAutoConfiguration.class})
public class WebMvcAutoConfigurationDisabler {
    // 仅用于禁用自动配置，无需其他内容
} 