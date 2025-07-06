package com.bgpay.bgai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * 完全隔离的WebFlux测试配置
 * 确保不会加载任何MVC相关配置
 */
@Configuration
@EnableWebFlux
@EnableAutoConfiguration(exclude = {
    // 排除所有与WebMVC相关的配置
    WebMvcAutoConfiguration.class,
    ErrorMvcAutoConfiguration.class,
    
    // 排除数据库相关配置
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class,
    
    // 排除ES相关配置
    ElasticsearchDataAutoConfiguration.class,
    ElasticsearchRepositoriesAutoConfiguration.class
})
@ImportAutoConfiguration(WebFluxAutoConfiguration.class)
@ComponentScan(
    basePackages = "com.bgpay.bgai",
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ANNOTATION,
            classes = EnableWebMvc.class
        ),
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                TestApplicationContext.class,
                AuthControllerTestConfig.class
            }
        )
    }
)
@Import({MockRepositoryConfig.class, NoLiquibaseTestConfig.class})
public class IsolatedWebFluxTestConfig implements WebFluxConfigurer {
    private static final Logger log = LoggerFactory.getLogger(IsolatedWebFluxTestConfig.class);
    
    public IsolatedWebFluxTestConfig() {
        log.info("初始化隔离的WebFlux测试配置");
        
        // 设置为响应式WebFlux模式
        System.setProperty("spring.main.web-application-type", "reactive");
        // 禁用文档生成
        System.setProperty("springdoc.api-docs.enabled", "false");
        System.setProperty("springdoc.swagger-ui.enabled", "false");
        // 禁用Seata和数据库相关功能
        System.setProperty("seata.enabled", "false");
        System.setProperty("seata.saga.state-machine.auto-register", "false");
        System.setProperty("spring.liquibase.enabled", "false");
        // 禁用不必要的监控指标
        System.setProperty("management.simple.metrics.export.enabled", "false");
        System.setProperty("management.metrics.enable.all", "false");
        
        // 显式禁用MVC相关配置
        System.setProperty("spring.mvc.enabled", "false");
        System.setProperty("spring.mvc.servlet.load-on-startup", "-1");
    }
    
    /**
     * 提供一个标志Bean，指示使用纯WebFlux模式
     */
    @Bean
    @Primary
    public Boolean webFluxOnlyMode() {
        return Boolean.TRUE;
    }
} 