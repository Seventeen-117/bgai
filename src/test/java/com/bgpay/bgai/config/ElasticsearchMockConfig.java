package com.bgpay.bgai.config;

import com.bgpay.bgai.model.es.ChatRecord;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

/**
 * Elasticsearch Mock配置类
 * 提供测试环境使用的Elasticsearch相关组件
 */
@TestConfiguration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ElasticsearchMockConfig {

    /**
     * 提供ElasticsearchClient mock
     */
    @Bean
    @Primary
    public ElasticsearchClient elasticsearchClient() {
        return Mockito.mock(ElasticsearchClient.class);
    }

    /**
     * 提供ReactiveElasticsearchClient mock
     */
    @Bean
    @Primary
    public ReactiveElasticsearchClient reactiveElasticsearchClient() {
        return Mockito.mock(ReactiveElasticsearchClient.class);
    }
    
    /**
     * 提供ElasticsearchOperations mock
     */
    @Bean
    @Primary
    public ElasticsearchOperations elasticsearchOperations() {
        return Mockito.mock(ElasticsearchOperations.class);
    }
    
    /**
     * 提供ElasticsearchTemplate mock
     */
    @Bean
    @Primary
    public ElasticsearchTemplate elasticsearchTemplate() {
        return Mockito.mock(ElasticsearchTemplate.class);
    }
    
    /**
     * 提供ClientConfiguration mock
     */
    @Bean
    @Primary
    public ClientConfiguration clientConfiguration() {
        return Mockito.mock(ClientConfiguration.class);
    }
} 