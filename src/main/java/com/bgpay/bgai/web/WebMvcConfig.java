package com.bgpay.bgai.web;

import com.bgpay.bgai.interceptor.LogTraceInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

/**
 * Spring MVC配置类
 * 添加拦截器和其他Web MVC相关配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LogTraceInterceptor logTraceInterceptor;

    /**
     * 添加拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加日志追踪拦截器
        registry.addInterceptor(logTraceInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/error/**");
    }
    
    /**
     * 提供WebServerApplicationContext Bean
     * 用于解决在测试环境中无法注入WebServerApplicationContext的问题
     */
    @Bean
    public WebServerApplicationContext webServerApplicationContext() {
        return new ServletWebServerApplicationContext();
    }
    
    /**
     * 提供ServletWebServerFactory Bean
     * 确保WebServerApplicationContext能够正常工作
     */
    @Bean
    public ServletWebServerFactory servletWebServerFactory() {
        return new TomcatServletWebServerFactory();
    }
} 