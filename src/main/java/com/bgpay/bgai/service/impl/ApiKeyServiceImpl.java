package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.config.ApiKeyConfig;
import com.bgpay.bgai.entity.ApiKeyInfo;
import com.bgpay.bgai.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyConfig apiKeyConfig;
    private final Map<String, ApiKeyInfo> apiKeyStore = new ConcurrentHashMap<>();

    @Override
    public ApiKeyInfo generateApiKey(String clientId, String clientName, String description) {
        // 生成API Key (使用UUID并移除连字符)
        String apiKey = UUID.randomUUID().toString().replace("-", "");
        
        // 创建API Key信息
        ApiKeyInfo apiKeyInfo = ApiKeyInfo.builder()
                .apiKey(apiKey)
                .clientId(clientId)
                .clientName(clientName)
                .description(description)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1)) // 默认一年有效期
                .active(true)
                .build();

        // 存储API Key信息
        apiKeyStore.put(apiKey, apiKeyInfo);
        
        // 更新配置
        updateApiKeyConfig();

        log.info("Generated new API Key for client: {}", clientId);
        return apiKeyInfo;
    }

    @Override
    public void revokeApiKey(String apiKey) {
        ApiKeyInfo apiKeyInfo = apiKeyStore.get(apiKey);
        if (apiKeyInfo != null) {
            apiKeyInfo.setActive(false);
            updateApiKeyConfig();
            log.info("Revoked API Key for client: {}", apiKeyInfo.getClientId());
        }
    }

    @Override
    public boolean validateApiKey(String apiKey) {
        ApiKeyInfo apiKeyInfo = apiKeyStore.get(apiKey);
        return apiKeyInfo != null && 
               apiKeyInfo.isActive() && 
               LocalDateTime.now().isBefore(apiKeyInfo.getExpiresAt());
    }

    @Override
    public List<ApiKeyInfo> getAllApiKeys() {
        return new ArrayList<>(apiKeyStore.values());
    }

    @Override
    public ApiKeyInfo getApiKeyInfo(String apiKey) {
        return apiKeyStore.get(apiKey);
    }

    @Override
    public void updateApiKeyStatus(String apiKey, boolean active) {
        ApiKeyInfo apiKeyInfo = apiKeyStore.get(apiKey);
        if (apiKeyInfo != null) {
            apiKeyInfo.setActive(active);
            updateApiKeyConfig();
            log.info("Updated API Key status for client: {}", apiKeyInfo.getClientId());
        }
    }

    private void updateApiKeyConfig() {
        // 更新ApiKeyConfig中的apiKeys映射
        Map<String, String> newApiKeys = apiKeyStore.entrySet().stream()
                .filter(entry -> entry.getValue().isActive() && 
                        LocalDateTime.now().isBefore(entry.getValue().getExpiresAt()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getClientId()
                ));
        
        // 使用反射更新配置
        try {
            java.lang.reflect.Field field = ApiKeyConfig.class.getDeclaredField("apiKeys");
            field.setAccessible(true);
            field.set(apiKeyConfig, newApiKeys);
        } catch (Exception e) {
            log.error("Failed to update API Key configuration", e);
        }
    }
} 