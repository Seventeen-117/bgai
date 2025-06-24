package com.bgpay.bgai.seata;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Seata安全认证配置
 * 解决"The get method not found for the field 'SeataProperties#accesskey'"警告
 * 注意：使用全小写属性名以匹配Seata内部期望
 */
@Configuration
@ConfigurationProperties(prefix = "seata")
@EnableConfigurationProperties
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
    
    // 明确提供getter和setter方法以解决警告
    public String getAccesskey() {
        return accesskey;
    }
    
    public void setAccesskey(String accesskey) {
        this.accesskey = accesskey;
    }
    
    public String getSecretkey() {
        return secretkey;
    }
    
    public void setSecretkey(String secretkey) {
        this.secretkey = secretkey;
    }
    
    public Security getSecurity() {
        return security;
    }
    
    public void setSecurity(Security security) {
        this.security = security;
    }
    
    public static class Security {
        private String accessKey = "";
        private String secretKey = "";
        private boolean authEnabled = false;
        
        // 明确提供getter和setter方法
        public String getAccessKey() {
            return accessKey;
        }
        
        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }
        
        public String getSecretKey() {
            return secretKey;
        }
        
        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }
        
        public boolean isAuthEnabled() {
            return authEnabled;
        }
        
        public void setAuthEnabled(boolean authEnabled) {
            this.authEnabled = authEnabled;
        }
    }
} 