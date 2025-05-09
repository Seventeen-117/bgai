package com.bgpay.bgai.entity;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class ApiKeyInfo {
    private String apiKey;
    private String clientId;
    private String clientName;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean active;
    private String description;
} 