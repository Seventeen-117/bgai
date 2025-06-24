package com.bgpay.bgai.config;

import com.bgpay.bgai.interceptor.LogTraceInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC配置类，用于注册拦截器等组件
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 创建日志追踪拦截器的Bean
     */
    @Bean
    public LogTraceInterceptor logTraceInterceptor() {
        return new LogTraceInterceptor();
    }

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册日志追踪拦截器，应用到所有请求路径
        registry.addInterceptor(logTraceInterceptor())
                .addPathPatterns("/**")
                // 排除不需要追踪的路径，如静态资源
                .excludePathPatterns("/webjars/**", "/swagger-ui/**", "/v3/api-docs/**", "/favicon.ico");
    }
} 