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
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.WebFilter;

/**
 * 完全独立的WebFlux测试配置
 * 不依赖任何其他配置类
 */
@Configuration
@EnableWebFlux
@EnableAutoConfiguration(exclude = {
    WebMvcAutoConfiguration.class,
    ErrorMvcAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class,
    ElasticsearchDataAutoConfiguration.class,
    ElasticsearchRepositoriesAutoConfiguration.class
})
@ImportAutoConfiguration(WebFluxAutoConfiguration.class)
@ComponentScan(
    basePackages = {
        "com.bgpay.bgai.controller",
        "com.bgpay.bgai.filter"
    },
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.bgpay\\.bgai\\.config\\..*"
        ),
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                TestApplicationContext.class,
                AuthControllerTestConfig.class,
                com.bgpay.bgai.controller.DynamicRouteController.class,
                com.bgpay.bgai.controller.SystemConfigController.class,
                com.bgpay.bgai.controller.EnhancedChatController.class
            }
        )
    }
)
public class WebFluxOnlyConfiguration implements WebFluxConfigurer {
    private static final Logger log = LoggerFactory.getLogger(WebFluxOnlyConfiguration.class);
    
    public WebFluxOnlyConfiguration() {
        log.info("初始化完全独立的WebFlux测试配置");
        
        // 设置为响应式WebFlux模式
        System.setProperty("spring.main.web-application-type", "reactive");
        
        // 显式禁用MVC相关配置
        System.setProperty("spring.mvc.enabled", "false");
        System.setProperty("spring.mvc.servlet.load-on-startup", "-1");
        
        // 显式启用WebFlux
        System.setProperty("spring.webflux.enabled", "true");
        
        // 禁用不必要的功能
        System.setProperty("springdoc.api-docs.enabled", "false");
        System.setProperty("springdoc.swagger-ui.enabled", "false");
        System.setProperty("seata.enabled", "false");
        System.setProperty("seata.saga.state-machine.auto-register", "false");
        System.setProperty("spring.liquibase.enabled", "false");
    }
    
    /**
     * 提供一个标志Bean，指示使用纯WebFlux模式
     */
    @Bean
    @Primary
    public Boolean webFluxOnlyMode() {
        return Boolean.TRUE;
    }
    
    /**
     * 提供一个空的幂等性过滤器
     */
    @Bean
    @Primary
    public WebFilter idempotenceWebFilter() {
        return (exchange, chain) -> chain.filter(exchange);
    }
} 