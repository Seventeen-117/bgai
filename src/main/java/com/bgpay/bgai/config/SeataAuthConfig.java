package com.bgpay.bgai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * Seata安全认证配置
 * 解决"The get method not found for the field 'SeataProperties#accesskey'"警告
 * 注意：使用全小写属性名以匹配Seata内部期望
 */
@Configuration
@ConfigurationProperties(prefix = "seata")
@EnableConfigurationProperties
@Data
public class SeataAuthConfig {
    
    /**
     * 访问密钥 - 使用全小写以匹配Seata内部期望
     */
    private String accesskey = "";
    
    /**
     * 安全密钥 - 使用全小写以匹配Seata内部期望
     */
    private String secretkey = "";
    
    /**
     * 安全配置嵌套类
     */
    private Security security = new Security();
    
    @Data
    public static class Security {
        private String accessKey = "";
        private String secretKey = "";
        private boolean authEnabled = false;
    }
} 