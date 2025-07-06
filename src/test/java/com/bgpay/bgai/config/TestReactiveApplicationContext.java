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
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import reactor.core.publisher.Mono;

/**
 * TestNG响应式测试应用上下文配置类
 * 用于配置WebFlux响应式测试环境
 */
@Configuration
@EnableDiscoveryClient
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableCaching
@EnableRetry
@EnableWebFlux
@EnableAutoConfiguration(exclude = {
    WebMvcAutoConfiguration.class,
    ElasticsearchDataAutoConfiguration.class,
    ElasticsearchRepositoriesAutoConfiguration.class
})
@ComponentScan(basePackages = {
        "org.apache.rocketmq.spring.autoconfigure",
        "org.apache.rocketmq.spring.core",
        "org.apache.rocketmq.spring.support",
        "com.bgpay.bgai"
})
@Import({TestNacosConfig.class, MockRepositoryConfig.class})
@TestPropertySource(locations = {
        "classpath:bootstrap.yml",
        "classpath:application.yml",
        "classpath:application-dev.yml"
}, properties = {
        "spring.main.web-application-type=reactive",
        "spring.main.allow-bean-definition-overriding=true",
        "spring.webflux.mvc-exclusion-prevent=false",
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
public class TestReactiveApplicationContext implements WebFluxConfigurer {
    private static final Logger log = LoggerFactory.getLogger(TestReactiveApplicationContext.class);
    
    /**
     * 手动创建ChatRecordWebFilter，使用模拟的Repository
     */
    @Bean
    @Primary
    public ChatRecordWebFilter chatRecordWebFilter(ChatRecordRepository chatRecordRepository) {
        log.info("创建ChatRecordWebFilter，使用模拟Repository");
        return new ChatRecordWebFilter(chatRecordRepository);
    }

    public TestReactiveApplicationContext() {
        log.info("初始化响应式TestNG应用上下文配置");
        
        // 设置系统参数，与主应用保持一致
        System.setProperty("spring.main.web-application-type", "reactive");
        System.setProperty("springdoc.api-docs.enabled", "false");
        System.setProperty("springdoc.swagger-ui.enabled", "false");
        System.setProperty("seata.saga.state-machine.auto-register", "false");
        System.setProperty("management.simple.metrics.export.enabled", "false");
        System.setProperty("management.metrics.enable.all", "false");
    }
}
