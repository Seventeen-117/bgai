package com.bgpay.bgai.service;

import com.bgpay.bgai.entity.ApiKeyInfo;
import java.util.List;

public interface ApiKeyService {
    ApiKeyInfo generateApiKey(String clientId, String clientName, String description);
    void revokeApiKey(String apiKey);
    boolean validateApiKey(String apiKey);
    List<ApiKeyInfo> getAllApiKeys();
    ApiKeyInfo getApiKeyInfo(String apiKey);
    void updateApiKeyStatus(String apiKey, boolean active);
} 