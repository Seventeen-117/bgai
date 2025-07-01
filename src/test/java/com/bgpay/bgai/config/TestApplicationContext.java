package com.bgpay.bgai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * TestNG测试应用上下文配置类
 * 用于配置测试环境中的Spring应用上下文
 */
@Configuration
@EnableDiscoveryClient
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableCaching
@EnableRetry
@ComponentScan(basePackages = {
        "org.apache.rocketmq.spring.autoconfigure",
        "org.apache.rocketmq.spring.core",
        "org.apache.rocketmq.spring.support",
        "com.bgpay.bgai"
})
@Import({TestNacosConfig.class})
@TestPropertySource(locations = {
        "classpath:bootstrap.yml",
        "classpath:application.yml",
        "classpath:application-dev.yml"
})
public class TestApplicationContext {
    private static final Logger log = LoggerFactory.getLogger(TestApplicationContext.class);

    public TestApplicationContext() {
        log.info("初始化TestNG应用上下文配置");
        
        // 设置系统参数，与主应用保持一致
        System.setProperty("seata.saga.state-machine.auto-register", "false");
        System.setProperty("management.simple.metrics.export.enabled", "false");
        System.setProperty("management.metrics.enable.all", "false");
    }
} 