package com.bgpay.bgai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "api.security")
public class ApiKeyConfig {
    private Map<String, String> apiKeys; // key: apiKey, value: clientId
    private String headerName = "X-API-Key"; // 默认的header名称
} 