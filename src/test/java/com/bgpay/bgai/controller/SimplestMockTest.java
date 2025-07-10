package com.bgpay.bgai.controller;

import com.bgpay.bgai.config.WebClientConfigOverrideConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.WebClient;
import org.testng.annotations.Test;

import javax.sql.DataSource;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 最简化测试
 * 仅包含必要的配置，用于测试WebClient模拟是否解决问题
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import({WebClientConfigOverrideConfig.class})
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.cloud.nacos.config.enabled=false",
    "spring.cloud.nacos.discovery.enabled=false",
    "rocketmq.enable=false",
    "rocketmq.name-server=none",
    "rocketmq.producer.group=none",
    "spring.elasticsearch.enabled=false",
    "spring.data.elasticsearch.repositories.enabled=false"
})
@ActiveProfiles("test")
public class SimplestMockTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private WebClient.Builder webClientBuilder;
    
    @MockBean
    private DataSource dataSource;
    
    @Test
    public void contextLoads() {
        // 简单测试，仅验证上下文能否加载
        assert webClientBuilder != null;
    }
    
    @Test
    public void testHealthEndpoint() throws Exception {
        // 简单测试健康检查端点
        mockMvc.perform(get("/actuator/health"))
               .andExpect(status().isOk());
    }
} 