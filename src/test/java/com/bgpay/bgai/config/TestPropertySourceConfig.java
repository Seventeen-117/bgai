package com.bgpay.bgai.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.TestPropertySource;

/**
 * 测试属性配置
 * 使用专门的测试配置文件
 */
@TestConfiguration
@Order(Integer.MIN_VALUE)
@TestPropertySource(locations = "classpath:application-test.yml")
public class TestPropertySourceConfig {
    // 只需提供TestPropertySource注解来加载测试配置文件
} 