package com.bgpay.bgai.service;

import com.bgpay.bgai.entity.ApiKey;
import com.bgpay.bgai.entity.ApiKeyInfo;
import java.time.LocalDateTime;
import java.util.List;

public interface ApiKeyService {
    enum ApiKeyStatus {
        VALID, EXPIRED, DISABLED, NOT_FOUND
    }
    class ApiKeyValidationResult {
        public ApiKeyStatus status;
        public LocalDateTime expiresAt;
        public String reason;
        public String clientId;
        public ApiKeyValidationResult(ApiKeyStatus status, LocalDateTime expiresAt, String reason, String clientId) {
            this.status = status;
            this.expiresAt = expiresAt;
            this.reason = reason;
            this.clientId = clientId;
        }
    }
    ApiKeyInfo generateApiKey(String clientId, String clientName, String description);
    void revokeApiKey(String apiKey);
    ApiKeyValidationResult validateApiKeyStatus(String apiKey);
    List<ApiKey> getAllApiKeys();
    ApiKey getApiKeyInfo(String apiKey);
    void updateApiKeyStatus(String apiKey, boolean active);
} 