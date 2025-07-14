package com.bgpay.bgai.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 提供MyBatisPlusConfig的Mock实现，用于测试环境
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    com.bgpay.bgai.config.MybatisPlusConfig.class,
    com.bgpay.bgai.datasource.MyBatisPlusConfig.class
})
public class MyBatisPlusConfigMock {

    /**
     * 提供一个Mock的MybatisPlusInterceptor bean，替代原始实现
     */
    @Bean
    @Primary
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        return Mockito.mock(MybatisPlusInterceptor.class);
    }
} 