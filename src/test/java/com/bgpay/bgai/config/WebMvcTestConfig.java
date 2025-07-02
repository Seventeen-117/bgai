package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

import jakarta.servlet.ServletContext;

/**
 * 测试配置类
 * 禁用所有可能导致问题的自动配置
 */
@Configuration
@TestPropertySource(locations = "classpath:application-auth-test.yml")
@EnableAutoConfiguration(exclude = {
    WebMvcAutoConfiguration.class,
    WebFluxAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    ElasticsearchDataAutoConfiguration.class,
    ElasticsearchRepositoriesAutoConfiguration.class,
    RedisAutoConfiguration.class
})
public class WebMvcTestConfig {
    
    /**
     * 提供MockServletContext
     */
    @Bean
    public ServletContext servletContext() {
        return new MockServletContext();
    }
    
    /**
     * 提供Web应用上下文
     */
    @Bean
    public WebApplicationContext webApplicationContext(ServletContext servletContext) {
        GenericWebApplicationContext context = new GenericWebApplicationContext();
        context.setServletContext(servletContext);
        return context;
    }
} 