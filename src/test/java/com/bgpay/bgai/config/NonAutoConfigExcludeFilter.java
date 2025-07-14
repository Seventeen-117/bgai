package com.bgpay.bgai.config;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 非自动配置类排除过滤器
 * 用于明确排除特定类，而不是通过@EnableAutoConfiguration的exclude属性
 * 解决"classes could not be excluded because they are not auto-configuration classes"错误
 */
public class NonAutoConfigExcludeFilter extends TypeExcludeFilter {

    private final Set<String> excludedClasses;

    /**
     * 默认构造函数，使用预定义的排除类列表
     */
    public NonAutoConfigExcludeFilter() {
        this.excludedClasses = new HashSet<>(Arrays.asList(
                "org.redisson.spring.starter.RedissonAutoConfiguration",
                "com.bgpay.bgai.web.WebClientConfig",
                "com.bgpay.bgai.config.GatewayRouteConfig"
        ));
    }

    /**
     * 自定义构造函数，接受要排除的类列表
     * @param excludedClassList 要排除的类名列表
     */
    public NonAutoConfigExcludeFilter(List<String> excludedClassList) {
        this.excludedClasses = excludedClassList != null ? 
                new HashSet<>(excludedClassList) : 
                Collections.emptySet();
    }

    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
        String className = metadataReader.getClassMetadata().getClassName();
        return excludedClasses.contains(className);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        NonAutoConfigExcludeFilter that = (NonAutoConfigExcludeFilter) obj;
        return excludedClasses.equals(that.excludedClasses);
    }
    
    @Override
    public int hashCode() {
        return excludedClasses.hashCode();
    }
} 