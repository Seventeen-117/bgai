package com.bgpay.bgai.controller;

import com.bgpay.bgai.config.WebClientMockConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.web.reactive.function.client.WebClient;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * 最小化WebFlux测试
 * 使用@WebFluxTest注解提供WebFlux上下文环境，排除其他所有自动配置
 */
@WebFluxTest
@Import(WebClientMockConfiguration.class)
@ActiveProfiles("test")
public class WebFluxMockTest extends AbstractTestNGSpringContextTests {
    
    @Autowired
    private ApplicationContext context;
    
    @Autowired
    private WebClient.Builder webClientBuilder;
    
    @Test
    public void contextLoads() {
        Assert.assertNotNull(context);
        System.out.println("Application context loaded successfully");
    }
    
    @Test
    public void webClientBuilderAvailable() {
        Assert.assertNotNull(webClientBuilder);
        
        // Test that builder methods work
        WebClient webClient = webClientBuilder
            .baseUrl("http://example.com")
            .build();
        
        Assert.assertNotNull(webClient);
        System.out.println("WebClient successfully created from builder");
    }
} 