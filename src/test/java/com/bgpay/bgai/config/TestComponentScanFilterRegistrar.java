package com.bgpay.bgai.config;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * 测试组件扫描过滤器注册器
 * 用于将TestComponentScanFilter注册到Spring上下文
 */
@Configuration
public class TestComponentScanFilterRegistrar {
    
    /**
     * 注册TestComponentScanFilter作为TypeExcludeFilter
     */
    @Bean
    @Order(Integer.MIN_VALUE) // 最高优先级
    public TypeExcludeFilter testComponentScanFilter() {
        return new TestComponentScanFilter();
    }
} 