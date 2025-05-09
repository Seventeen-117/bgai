package com.bgpay.bgai.controller;

import com.bgpay.bgai.entity.ApiKeyInfo;
import com.bgpay.bgai.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping("/generate")
    public ResponseEntity<ApiKeyInfo> generateApiKey(
            @RequestBody Map<String, String> request) {
        String clientId = request.get("clientId");
        String clientName = request.get("clientName");
        String description = request.get("description");

        ApiKeyInfo apiKeyInfo = apiKeyService.generateApiKey(clientId, clientName, description);
        return ResponseEntity.ok(apiKeyInfo);
    }

    @PostMapping("/{apiKey}/revoke")
    public ResponseEntity<Void> revokeApiKey(@PathVariable String apiKey) {
        apiKeyService.revokeApiKey(apiKey);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyInfo>> getAllApiKeys() {
        return ResponseEntity.ok(apiKeyService.getAllApiKeys());
    }

    @GetMapping("/{apiKey}")
    public ResponseEntity<ApiKeyInfo> getApiKeyInfo(@PathVariable String apiKey) {
        ApiKeyInfo apiKeyInfo = apiKeyService.getApiKeyInfo(apiKey);
        return apiKeyInfo != null ? 
                ResponseEntity.ok(apiKeyInfo) : 
                ResponseEntity.notFound().build();
    }

    @PutMapping("/{apiKey}/status")
    public ResponseEntity<Void> updateApiKeyStatus(
            @PathVariable String apiKey,
            @RequestBody Map<String, Boolean> request) {
        Boolean active = request.get("active");
        if (active != null) {
            apiKeyService.updateApiKeyStatus(apiKey, active);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }
} 