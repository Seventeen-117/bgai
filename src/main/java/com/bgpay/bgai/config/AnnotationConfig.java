package com.bgpay.bgai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.AnnotationUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.annotation.Annotation;

/**
 * 注解配置类，确保javax.annotation包中的注解可以正确使用
 */
@Configuration
public class AnnotationConfig {
    
    /**
     * 确认javax.annotation.PostConstruct注解可用
     */
    @PostConstruct
    public void init() {
        // 什么都不做，只是确保javax.annotation.PostConstruct可以正确加载
    }
} 