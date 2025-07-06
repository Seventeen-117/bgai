package com.bgpay.bgai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Seata自动配置排除过滤器
 * 在自动配置导入阶段直接排除Seata相关配置，避免类加载和初始化
 */
@Component
@Order(Integer.MIN_VALUE)
public class SeataAutoConfigurationExclusionFilter implements AutoConfigurationImportFilter {
    private static final Logger log = LoggerFactory.getLogger(SeataAutoConfigurationExclusionFilter.class);
    
    // 需要排除的Seata相关配置类
    private static final Set<String> EXCLUDED_AUTO_CONFIGURATIONS = new HashSet<>();
    
    static {
        EXCLUDED_AUTO_CONFIGURATIONS.add("io.seata.spring.boot.autoconfigure.SeataAutoConfiguration");
        EXCLUDED_AUTO_CONFIGURATIONS.add("io.seata.spring.boot.autoconfigure.SeataDataSourceAutoConfiguration");
        EXCLUDED_AUTO_CONFIGURATIONS.add("io.seata.spring.boot.autoconfigure.SeataHttpAutoConfiguration");
        EXCLUDED_AUTO_CONFIGURATIONS.add("io.seata.spring.boot.autoconfigure.SeataFeignClientAutoConfiguration");
        EXCLUDED_AUTO_CONFIGURATIONS.add("io.seata.spring.boot.autoconfigure.SeataRestTemplateAutoConfiguration");
        EXCLUDED_AUTO_CONFIGURATIONS.add("io.seata.spring.boot.autoconfigure.SeataWebMvcAutoConfiguration");
        EXCLUDED_AUTO_CONFIGURATIONS.add("io.seata.spring.boot.autoconfigure.SeataMetricsAutoConfiguration");
        log.info("初始化Seata自动配置排除过滤器，将排除所有Seata相关配置");
    }
    
    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean[] match = new boolean[autoConfigurationClasses.length];
        
        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            String autoConfigClass = autoConfigurationClasses[i];
            // 默认允许，除非是在排除列表中
            match[i] = !EXCLUDED_AUTO_CONFIGURATIONS.contains(autoConfigClass);
            
            // 特别标记被排除的Seata配置
            if (!match[i]) {
                log.info("已排除Seata自动配置类: {}", autoConfigClass);
            }
        }
        
        return match;
    }
} 