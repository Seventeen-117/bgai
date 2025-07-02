package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.bgpay.bgai.controller.AuthController;
import com.bgpay.bgai.service.UserService;

import jakarta.servlet.ServletContext;

/**
 * Auth控制器测试专用配置
 * 禁用WebFlux组件，避免与MockMvc冲突
 */
@Configuration
@EnableWebMvc
@EnableAutoConfiguration(exclude = {
    WebFluxAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    io.seata.spring.boot.autoconfigure.SeataAutoConfiguration.class
})
@TestPropertySource(properties = {
    "spring.main.web-application-type=servlet",
    "springdoc.api-docs.enabled=false",
    "springdoc.swagger-ui.enabled=false",
    "seata.enabled=false",
    "spring.cloud.alibaba.seata.enabled=false"
})
public class AuthControllerTestConfig {
    
    /**
     * 提供模拟的Servlet上下文
     */
    @Bean
    public ServletContext servletContext() {
        return new MockServletContext();
    }
    
    /**
     * 配置DispatcherServlet，并注册到ServletContext
     */
    @Bean
    public ServletRegistrationBean<DispatcherServlet> dispatcherServletRegistration() {
        ServletRegistrationBean<DispatcherServlet> registration = new ServletRegistrationBean<>(
                dispatcherServlet());
        registration.setLoadOnStartup(1);
        registration.addUrlMappings("/*");
        return registration;
    }
    
    /**
     * 创建DispatcherServlet实例
     */
    @Bean
    public DispatcherServlet dispatcherServlet() {
        return new DispatcherServlet();
    }
    
    /**
     * 提供UserService的Mock实例
     */
    @Bean
    @Primary
    public UserService userService() {
        return Mockito.mock(UserService.class);
    }
} 