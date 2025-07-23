package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

/**
 * 简化的测试配置类
 * 使用最小化配置进行测试，避免不必要的组件加载
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    ElasticsearchRestClientAutoConfiguration.class,
    ElasticsearchDataAutoConfiguration.class,
    ReactiveElasticsearchRepositoriesAutoConfiguration.class,
    ElasticsearchRepositoriesAutoConfiguration.class,
    org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration.class
})
@TestPropertySource(locations = "classpath:application-test.yml")
@ComponentScan(basePackages = {"com.bgpay.bgai.config"})
public class SimpleTestConfig {
    
    // 提供主要的Environment Bean，避免冲突
    @Bean
    @Primary
    public org.springframework.core.env.Environment environment() {
        return org.springframework.mock.env.MockEnvironment.class.cast(
            new org.springframework.mock.env.MockEnvironment()
                .withProperty("rocketmq.name-server", "127.0.0.1:9876")
                .withProperty("rocketmq.producer.group", "test-group")
                .withProperty("rocketmq.enable", "false")
                .withProperty("rocketmq.messageConsumer.enabled", "false")
        );
    }
} 