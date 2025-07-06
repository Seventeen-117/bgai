package com.bgpay.bgai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * 专门用于WebFlux测试的Spring Boot应用配置
 * 完全隔离所有MVC相关配置
 */
@SpringBootApplication(exclude = {
    WebMvcAutoConfiguration.class,
    ErrorMvcAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class,
    ElasticsearchDataAutoConfiguration.class,
    ElasticsearchRepositoriesAutoConfiguration.class
})
@EnableWebFlux
@ComponentScan(
    basePackages = "com.bgpay.bgai",
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ANNOTATION,
            classes = EnableWebMvc.class
        )
    }
)
public class WebFluxTestApplication {
    private static final Logger log = LoggerFactory.getLogger(WebFluxTestApplication.class);
    
    public WebFluxTestApplication() {
        log.info("初始化WebFlux测试应用配置");
        
        // 设置为响应式WebFlux模式
        System.setProperty("spring.main.web-application-type", "reactive");
        
        // 显式禁用MVC相关配置
        System.setProperty("spring.mvc.enabled", "false");
        System.setProperty("spring.mvc.servlet.load-on-startup", "-1");
        
        // 显式启用WebFlux
        System.setProperty("spring.webflux.enabled", "true");
    }
} 