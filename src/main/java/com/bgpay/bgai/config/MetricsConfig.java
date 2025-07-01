package com.bgpay.bgai.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Metrics配置类
 * 用于处理Micrometer相关配置，解决应用关闭时的bean创建错误
 */
@Configuration
public class MetricsConfig {

    /**
     * 提供一个空的MeterRegistry，避免在应用关闭时创建新的bean
     */
    @Bean
    @Primary
    public MeterRegistry meterRegistry() {
        return new CompositeMeterRegistry();
    }

    /**
     * 自定义MeterRegistry，禁用所有指标收集
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            // 不添加任何标签，实际上不会被使用
        };
    }
} 