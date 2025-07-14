package com.bgpay.bgai.config;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 测试组件扫描过滤器
 * 用于从组件扫描中排除特定的类
 * 主要用于测试环境下排除会引起问题的配置类
 */
@Component
public class TestComponentScanFilter extends TypeExcludeFilter {
    
    // 要排除的类名列表
    private static final Set<String> EXCLUDED_CLASSES = new HashSet<>(Arrays.asList(
            "com.bgpay.bgai.config.GatewayRouteConfig"
    ));
    
    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
        String className = metadataReader.getClassMetadata().getClassName();
        return EXCLUDED_CLASSES.contains(className);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
} 