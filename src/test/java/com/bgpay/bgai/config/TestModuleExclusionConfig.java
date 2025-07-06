package com.bgpay.bgai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;

/**
 * 测试模块排除配置
 * 用于在WebFlux测试中排除MVC相关的配置类和组件
 * 同时排除Seata自动配置以避免"service.vgroupMapping.default_tx_group configuration item is required"错误
 */
@Configuration
@ComponentScan(
    basePackages = "com.bgpay.bgai",
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                TestApplicationContext.class,
                AuthControllerTestConfig.class,
                WebMvcTestConfig.class
            }
        ),
        @ComponentScan.Filter(
            type = FilterType.ANNOTATION,
            classes = org.springframework.web.servlet.config.annotation.EnableWebMvc.class
        )
    }
)
@EnableAutoConfiguration(exclude = {
    WebMvcAutoConfiguration.class,
    ErrorMvcAutoConfiguration.class,
    // 排除Seata自动配置
    io.seata.spring.boot.autoconfigure.SeataAutoConfiguration.class
})
@Profile("idempotence-test")
public class TestModuleExclusionConfig {
    private static final Logger log = LoggerFactory.getLogger(TestModuleExclusionConfig.class);
    
    public TestModuleExclusionConfig() {
        log.info("初始化测试模块排除配置，排除所有MVC相关配置和Seata自动配置");
    }
    
    /**
     * 标记Bean，表示已经排除了MVC配置
     */
    @Bean
    public Boolean mvcConfigurationsExcluded() {
        return Boolean.TRUE;
    }

    /**
     * 标记Bean，表示已经排除了Seata配置
     */
    @Bean
    public Boolean seataConfigurationsExcluded() {
        return Boolean.TRUE;
    }
} 