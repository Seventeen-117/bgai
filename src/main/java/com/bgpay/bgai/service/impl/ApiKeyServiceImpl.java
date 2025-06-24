package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.entity.ApiClient;
import com.bgpay.bgai.entity.ApiKey;
import com.bgpay.bgai.mapper.ApiClientMapper;
import com.bgpay.bgai.mapper.ApiKeyMapper;
import com.bgpay.bgai.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bgpay.bgai.utils.Sha256Util;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyServiceImpl implements ApiKeyService {
    private final ApiClientMapper apiClientMapper;
    private final ApiKeyMapper apiKeyMapper;

    @Override
    @Transactional
    public ApiKey generateApiKey(String clientId, String clientName, String description) {
        // 校验 clientId 是否存在且启用
        ApiClient client = apiClientMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ApiClient>()
                .eq("client_id", clientId)
                .eq("status", 1)
        );
        if (client == null) {
            throw new IllegalArgumentException("无效或未启用的 clientId");
        }
        // 生成明文API Key
        String plainApiKey = UUID.randomUUID().toString().replace("-", "");
        // 用SHA-256哈希后存库
        String hashedApiKey = Sha256Util.hash(plainApiKey);
        ApiKey apiKey = new ApiKey();
        apiKey.setApiKey(hashedApiKey);
        apiKey.setClientId(clientId);
        apiKey.setClientName(client.getClientName());
        apiKey.setDescription(description);
        apiKey.setCreatedAt(LocalDateTime.now());
        apiKey.setExpiresAt(LocalDateTime.now().plusYears(1));
        apiKey.setActive(1);
        apiKeyMapper.insert(apiKey);
        log.info("Generated new API Key for client: {} (SHA-256哈希存储)", clientId);
        ApiKey result = new ApiKey();
        result.setApiKey(plainApiKey);
        result.setClientId(clientId);
        result.setClientName(client.getClientName());
        result.setDescription(description);
        result.setCreatedAt(apiKey.getCreatedAt());
        result.setExpiresAt(apiKey.getExpiresAt());
        result.setActive(1);
        return result;
    }

    @Override
    public void revokeApiKey(String apiKey) {
        ApiKey key = apiKeyMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ApiKey>()
                .eq("api_key", apiKey)
                .eq("active", 1)
        );
        if (key != null) {
            key.setActive(0);
            apiKeyMapper.updateById(key);
            log.info("Revoked API Key: {}", apiKey);
        }
    }

    @Override
    public ApiKeyValidationResult validateApiKeyStatus(String apiKey) {
        String hashedApiKey = Sha256Util.hash(apiKey);
        ApiKey key = apiKeyMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ApiKey>()
                .eq("api_key", hashedApiKey)
        );
        if (key == null) {
            return new ApiKeyValidationResult(ApiKeyStatus.NOT_FOUND, null, "API Key not found", null);
        }
        if (key.getActive() == null || key.getActive() == 0) {
            return new ApiKeyValidationResult(ApiKeyStatus.DISABLED, key.getExpiresAt(), "API Key disabled", key.getClientId());
        }
        if (key.getExpiresAt() == null || key.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            return new ApiKeyValidationResult(ApiKeyStatus.EXPIRED, key.getExpiresAt(), "API Key expired", key.getClientId());
        }
        return new ApiKeyValidationResult(ApiKeyStatus.VALID, key.getExpiresAt(), null, key.getClientId());
    }

    @Override
    public java.util.List<ApiKey> getAllApiKeys() {
        // 不返回明文apiKey，apiKey字段为hash
        return apiKeyMapper.selectList(null);
    }

    @Override
    public ApiKey getApiKeyInfo(String apiKey) {
        String hashedApiKey = Sha256Util.hash(apiKey);
        return apiKeyMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ApiKey>()
                .eq("api_key", hashedApiKey)
        );
    }

    @Override
    public void updateApiKeyStatus(String apiKey, boolean active) {
        ApiKey key = apiKeyMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ApiKey>()
                .eq("api_key", apiKey)
        );
        if (key != null) {
            key.setActive(active ? 1 : 0);
            apiKeyMapper.updateById(key);
            log.info("Updated API Key status: {} -> {}", apiKey, active);
        }
    }
} 