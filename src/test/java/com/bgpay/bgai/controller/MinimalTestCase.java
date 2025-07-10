package com.bgpay.bgai.controller;

import com.bgpay.bgai.config.WebClientMockConfiguration;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 极简测试用例
 * 只测试单个组件，避免自动配置问题
 */
@WebFluxTest
@ContextConfiguration(classes = {WebClientMockConfiguration.class})
@TestPropertySource(locations = "classpath:application-test.yml")
@ActiveProfiles("test")
public class MinimalTestCase extends AbstractTestNGSpringContextTests {

    @Autowired
    private ApplicationContext context;
    
    @Autowired
    private WebTestClient webTestClient;
    
    private WebClient webClient;
    
    @BeforeClass
    public void setUp() {
        webClient = Mockito.mock(WebClient.class);
    }

    @Test
    public void testContextLoads() {
        System.out.println("Application context loaded successfully");
        assert context != null;
        assert webClient != null;
    }
} 