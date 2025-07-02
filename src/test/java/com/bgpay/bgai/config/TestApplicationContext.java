package com.bgpay.bgai.config;

import com.bgpay.bgai.filter.ChatRecordWebFilter;
import com.bgpay.bgai.model.es.ChatRecord;
import com.bgpay.bgai.repository.es.ChatRecordRepository;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockServletContext;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import jakarta.servlet.ServletContext;

/**
 * TestNG测试应用上下文配置类
 * 用于配置测试环境中的Spring应用上下文
 */
@Configuration
@EnableDiscoveryClient
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableCaching
@EnableRetry
@EnableWebMvc
@EnableAutoConfiguration(exclude = {
    WebFluxAutoConfiguration.class,
    ElasticsearchDataAutoConfiguration.class,
    ElasticsearchRepositoriesAutoConfiguration.class
})
@ComponentScan(basePackages = {
        "org.apache.rocketmq.spring.autoconfigure",
        "org.apache.rocketmq.spring.core",
        "org.apache.rocketmq.spring.support",
        "com.bgpay.bgai"
})
@Import({TestNacosConfig.class})
@TestPropertySource(locations = {
        "classpath:bootstrap.yml",
        "classpath:application.yml",
        "classpath:application-dev.yml"
}, properties = {
        "spring.main.web-application-type=servlet",
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
public class TestApplicationContext {
    private static final Logger log = LoggerFactory.getLogger(TestApplicationContext.class);
    
    /**
     * 提供模拟的Servlet上下文
     */
    @Bean
    public ServletContext servletContext() {
        return new MockServletContext();
    }
    
    /**
     * 提供模拟的ChatRecordRepository
     * 避免Elasticsearch依赖问题
     */
    @Bean
    @Primary
    public ChatRecordRepository mockChatRecordRepository() {
        log.info("创建模拟ChatRecordRepository");
        ChatRecordRepository mockRepository = Mockito.mock(ChatRecordRepository.class);
        // 配置mock行为
        Mockito.when(mockRepository.save(Mockito.any(ChatRecord.class)))
               .thenAnswer(invocation -> invocation.getArgument(0));
        return mockRepository;
    }
    
    /**
     * 手动创建ChatRecordWebFilter，使用模拟的Repository
     */
    @Bean
    @Primary
    public ChatRecordWebFilter chatRecordWebFilter(ChatRecordRepository chatRecordRepository) {
        log.info("创建ChatRecordWebFilter，使用模拟Repository");
        return new ChatRecordWebFilter(chatRecordRepository);
    }

    public TestApplicationContext() {
        log.info("初始化TestNG应用上下文配置");
        
        // 设置系统参数，与主应用保持一致
        System.setProperty("spring.main.web-application-type", "servlet");
        System.setProperty("springdoc.api-docs.enabled", "false");
        System.setProperty("springdoc.swagger-ui.enabled", "false");
        System.setProperty("seata.saga.state-machine.auto-register", "false");
        System.setProperty("management.simple.metrics.export.enabled", "false");
        System.setProperty("management.metrics.enable.all", "false");
    }
} 