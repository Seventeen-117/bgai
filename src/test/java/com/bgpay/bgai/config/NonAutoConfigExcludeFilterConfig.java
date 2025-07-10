package com.bgpay.bgai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * 非自动配置排除过滤器配置
 * 从配置文件中读取要排除的类，并配置相应的过滤器
 */
@TestConfiguration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NonAutoConfigExcludeFilterConfig {

    /**
     * 从配置文件中读取要排除的非自动配置类列表
     */
    @Value("${non-auto-configuration.excluded-classes:}")
    private List<String> excludedClasses;

    /**
     * 创建TypeExcludeFilter的扩展，用于排除指定的类
     */
    @Bean
    @Primary
    public TypeExcludeFilter nonAutoConfigExcludeFilter() {
        return new NonAutoConfigExcludeFilter(excludedClasses);
    }
} 