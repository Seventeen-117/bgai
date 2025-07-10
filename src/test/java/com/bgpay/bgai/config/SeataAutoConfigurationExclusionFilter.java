package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.core.annotation.Order;

import java.util.HashSet;
import java.util.Set;

/**
 * Seata自动配置排除过滤器
 * 用于在测试环境中排除Seata相关的自动配置，避免测试时加载Seata相关组件
 */
@Order(Integer.MIN_VALUE)
public class SeataAutoConfigurationExclusionFilter implements AutoConfigurationImportFilter {

    // 需要排除的Seata相关自动配置类
    private static final Set<String> EXCLUDE_AUTO_CONFIGURATIONS = new HashSet<>();

    static {
        // 添加需要排除的Seata自动配置类
        EXCLUDE_AUTO_CONFIGURATIONS.add("io.seata.spring.boot.autoconfigure.SeataAutoConfiguration");
        EXCLUDE_AUTO_CONFIGURATIONS.add("io.seata.spring.boot.autoconfigure.SeataDataSourceAutoConfiguration");
        EXCLUDE_AUTO_CONFIGURATIONS.add("io.seata.saga.engine.impl.DefaultStateMachineConfig");
        EXCLUDE_AUTO_CONFIGURATIONS.add("io.seata.spring.annotation.GlobalTransactionScanner");
        EXCLUDE_AUTO_CONFIGURATIONS.add("io.seata.spring.annotation.GlobalTransactionalInterceptor");
        EXCLUDE_AUTO_CONFIGURATIONS.add("io.seata.spring.annotation.datasource.SeataAutoDataSourceProxyCreator");
        EXCLUDE_AUTO_CONFIGURATIONS.add("io.seata.spring.boot.autoconfigure.SeataCoreAutoConfiguration");
        EXCLUDE_AUTO_CONFIGURATIONS.add("io.seata.spring.boot.autoconfigure.SeataHttpAutoConfiguration");
        EXCLUDE_AUTO_CONFIGURATIONS.add("io.seata.spring.boot.autoconfigure.SeataServerAutoConfiguration");
    }

    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean[] matches = new boolean[autoConfigurationClasses.length];
        
        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            String autoConfigurationClass = autoConfigurationClasses[i];
            
            // 默认允许加载，除非在排除列表中
            matches[i] = !EXCLUDE_AUTO_CONFIGURATIONS.contains(autoConfigurationClass);
        }
        
        return matches;
    }
} 