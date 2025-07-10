package com.bgpay.bgai.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.support.DefaultServerCodecConfigurer;
import org.springframework.web.server.WebFilter;

import java.util.Collections;
import java.util.List;

/**
 * 反应式Web组件的Mock配置
 * 提供各种反应式Web所需的组件，避免相关Bean缺失的问题
 */
@TestConfiguration
public class ReactiveWebMockConfig {

    /**
     * 提供一个默认的数据缓冲区工厂
     */
    @Bean
    @Primary
    public DataBufferFactory dataBufferFactory() {
        return new DefaultDataBufferFactory();
    }

    /**
     * 提供一个ServerCodecConfigurer实例
     */
    @Bean
    @Primary
    public ServerCodecConfigurer serverCodecConfigurer() {
        return new DefaultServerCodecConfigurer();
    }

    /**
     * 提供一个空的WebFilter列表
     * 避免WebFilter相关的依赖问题
     */
    @Bean
    @Primary
    public List<WebFilter> webFilters() {
        return Collections.emptyList();
    }
} 