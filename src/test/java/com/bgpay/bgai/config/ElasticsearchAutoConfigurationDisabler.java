package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * 禁用Elasticsearch自动配置
 * 用于测试环境避免Elasticsearch相关配置的加载
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    ElasticsearchDataAutoConfiguration.class,
    ElasticsearchRepositoriesAutoConfiguration.class
})
public class ElasticsearchAutoConfigurationDisabler {
} 