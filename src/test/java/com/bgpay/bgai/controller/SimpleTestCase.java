package com.bgpay.bgai.controller;

import com.bgpay.bgai.BgaiApplication;
import com.bgpay.bgai.config.WebClientConfigMock;
import com.bgpay.bgai.config.WebClientMockConfiguration;
import com.bgpay.bgai.config.ElasticsearchMockConfig;
import com.bgpay.bgai.config.ChatRecordRepositoryMockConfig;
import com.bgpay.bgai.config.RocketMQMockConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.web.servlet.MockMvc;
import org.testng.annotations.Test;

/**
 * 简化的测试用例
 * 使用最小配置进行测试，仅验证基本功能
 */
@SpringBootTest(classes = {SimpleTestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({
    WebClientConfigMock.class,
    WebClientMockConfiguration.class,
    ElasticsearchMockConfig.class,
    ChatRecordRepositoryMockConfig.class,
    RocketMQMockConfig.class
})
public class SimpleTestCase extends AbstractTestNGSpringContextTests {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    /**
     * 简单测试 - 验证上下文是否正常加载
     */
    @Test
    public void testContextLoads() {
        System.out.println("Application context loaded successfully");
        assert context != null;
    }
} 