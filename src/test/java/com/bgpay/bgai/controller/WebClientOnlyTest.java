package com.bgpay.bgai.controller;

import com.bgpay.bgai.config.RocketMQExclusionConfig;
import com.bgpay.bgai.config.WebClientMockConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.web.reactive.function.client.WebClient;
import org.testng.annotations.Test;

import javax.annotation.Resource;

/**
 * WebClient专项测试
 * 仅测试WebClient相关配置，不依赖其他外部组件
 */
@SpringBootTest(classes = {WebClientMockConfiguration.class, RocketMQExclusionConfig.class})
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "spring.cloud.nacos.config.enabled=false",
    "spring.cloud.nacos.discovery.enabled=false",
    "spring.data.elasticsearch.repositories.enabled=false",
    "spring.elasticsearch.enabled=false",
    "rocketmq.enable=false",
    "rocketmq.name-server=none",
    "rocketmq.producer.group=none",
    "spring.cloud.loadbalancer.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "spring.cloud.config.enabled=false",
    "spring.cloud.consul.enabled=false",
    "spring.cloud.gateway.enabled=false",
    "spring.cloud.circuitbreaker.enabled=false",
    "spring.data.elasticsearch.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration,org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration"
})
@ActiveProfiles("test")
public class WebClientOnlyTest extends AbstractTestNGSpringContextTests {

    @Resource
    private WebClient.Builder webClientBuilder;
    
    /**
     * 测试WebClient.Builder是否正常注入
     */
    @Test
    public void testWebClientBuilder() {
        assert webClientBuilder != null;
        System.out.println("WebClient.Builder successfully injected");
        
        // 测试builder能够正常构建WebClient
        WebClient webClient = webClientBuilder
            .baseUrl("http://example.com")
            .defaultHeader("Content-Type", "application/json")
            .build();
        
        assert webClient != null;
        System.out.println("WebClient successfully built from builder");
    }
} 