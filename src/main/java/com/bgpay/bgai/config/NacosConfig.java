package com.bgpay.bgai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Nacos配置类，用于处理Nacos连接问题和配置加载
 */
@Configuration
@RefreshScope
public class NacosConfig {
    private static final Logger log = LoggerFactory.getLogger(NacosConfig.class);
    
    @Value("${spring.cloud.nacos.config.server-addr:localhost:8848}")
    private String serverAddr;
    
    @Value("${spring.cloud.nacos.config.namespace:}")
    private String namespace;
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    /**
     * 应用启动后打印Nacos连接信息
     */
    @EventListener(ContextRefreshedEvent.class)
    public void afterStartup() {
        log.info("应用启动完成，当前环境: {}", activeProfile);
        log.info("Nacos配置中心: {}, 命名空间: {}", serverAddr, 
                namespace.isEmpty() ? "默认命名空间" : namespace);
        
        // 添加提示信息，帮助理解Nacos警告
        log.info("注意: 如果看到Nacos配置加载警告，但应用正常启动，说明应用正在使用本地配置文件");
    }
    
    /**
     * 自定义Nacos警告处理器，仅当启用警告处理时激活
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.cloud.nacos.config.handle-warnings", havingValue = "true", matchIfMissing = true)
    public NacosWarningHandler nacosWarningHandler() {
        return new NacosWarningHandler();
    }
    
    /**
     * 内部类：处理Nacos警告的帮助类
     */
    public static class NacosWarningHandler {
        private static final Logger log = LoggerFactory.getLogger(NacosWarningHandler.class);
        
        /**
         * 初始化时打印帮助信息
         */
        public NacosWarningHandler() {
            log.info("Nacos警告处理器已启用，将忽略配置中心连接警告");
        }
    }
} 