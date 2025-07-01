package com.bgpay.bgai.config;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.JvmMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.LogbackMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.SystemMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Actuator配置类
 * 用于自定义Actuator配置，解决应用关闭时的bean创建错误
 */
@Configuration
@AutoConfigureBefore({
    MetricsAutoConfiguration.class,
    CompositeMeterRegistryAutoConfiguration.class,
    SimpleMetricsExportAutoConfiguration.class,
    JvmMetricsAutoConfiguration.class,
    LogbackMetricsAutoConfiguration.class,
    SystemMetricsAutoConfiguration.class
})
public class ActuatorConfig {

    /**
     * 创建一个空的关闭钩子，替代默认的meterRegistryCloser
     * 这样可以避免应用关闭时尝试创建新的bean
     */
    @Bean
    public Object meterRegistryCloser() {
        // 返回一个空对象，不执行任何操作
        return new Object();
    }
} 