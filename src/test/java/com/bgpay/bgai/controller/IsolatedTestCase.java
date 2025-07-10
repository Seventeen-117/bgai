package com.bgpay.bgai.controller;

import com.bgpay.bgai.config.NonAutoConfigExcludeFilter;
import com.bgpay.bgai.config.NonAutoConfigExcludeFilterConfig;
import com.bgpay.bgai.config.WebClientMockConfiguration;
import com.bgpay.bgai.web.WebClientConfig;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.web.reactive.function.client.WebClient;
import org.testng.annotations.Test;

/**
 * 完全隔离的测试用例
 * 禁用所有外部依赖，专注于测试WebClient配置
 */
@SpringBootTest(classes = IsolatedTestCase.TestConfig.class)
@TestPropertySource(properties = {
    "spring.cloud.nacos.config.enabled=false",
    "spring.cloud.nacos.discovery.enabled=false",
    "spring.data.elasticsearch.repositories.enabled=false",
    "spring.elasticsearch.enabled=false",
    "rocketmq.enable=false",
    "spring.cloud.loadbalancer.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "spring.cloud.config.enabled=false",
    "spring.cloud.consul.enabled=false",
    "spring.cloud.gateway.enabled=false",
    "spring.cloud.circuitbreaker.enabled=false",
    "seata.enabled=false",
    "saga.enabled=false",
    // 明确指定要排除的非自动配置类列表
    "non-auto-configuration.excluded-classes[0]=org.redisson.spring.starter.RedissonAutoConfiguration",
    "non-auto-configuration.excluded-classes[1]=com.bgpay.bgai.web.WebClientConfig",
    "non-auto-configuration.excluded-classes[2]=com.bgpay.bgai.config.GatewayRouteConfig"
})
@ActiveProfiles("test")
public class IsolatedTestCase extends AbstractTestNGSpringContextTests {

    @Autowired(required = false)
    private ApplicationContext context;
    
    @Autowired(required = false)
    private WebClient webClient;

    @Test
    public void testContextLoads() {
        System.out.println("Application context loaded successfully");
        assert context != null;
    }
    
    /**
     * 隔离的测试配置
     * 禁用所有数据库和外部服务依赖
     */
    @Configuration
    @EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        ElasticsearchRestClientAutoConfiguration.class,
        ElasticsearchDataAutoConfiguration.class,
        ReactiveElasticsearchRepositoriesAutoConfiguration.class,
        ElasticsearchRepositoriesAutoConfiguration.class,
        WebClientAutoConfiguration.class
    })
    @Import({
        WebClientMockConfiguration.class,
        NonAutoConfigExcludeFilterConfig.class  // 导入非自动配置类过滤器配置
    })
    public static class TestConfig {
        
        @Bean
        @Primary
        public WebClientConfig webClientConfig() {
            return Mockito.mock(WebClientConfig.class);
        }
        
        @Bean
        @Primary
        public WebClient webClient() {
            return Mockito.mock(WebClient.class);
        }
    }
} 