package com.bgpay.bgai.controller;

import com.bgpay.bgai.entity.ApiKeyInfo;
import com.bgpay.bgai.entity.ApiKey;
import com.bgpay.bgai.entity.UserToken;
import com.bgpay.bgai.service.ApiKeyService;
import com.bgpay.bgai.service.UserService;
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
    private final UserService userService;

    @PostMapping("/generate")
    public ResponseEntity<?> generateApiKey(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, String> request) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
        }
        String accessToken = authorization.replace("Bearer ", "");
        UserToken userToken = userService.validateToken(accessToken);
        if (userToken == null || !userToken.isValid() || userToken.getTokenExpireTime() == null || userToken.getTokenExpireTime().isBefore(java.time.LocalDateTime.now())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
        }
        String clientId = request.get("clientId");
        String clientName = request.get("clientName");
        String description = request.get("description");
        ApiKey apiKey = apiKeyService.generateApiKey(clientId, clientName, description);
        return ResponseEntity.ok(apiKey);
    }

    @PostMapping("/{apiKey}/revoke")
    public ResponseEntity<Void> revokeApiKey(@PathVariable String apiKey) {
        apiKeyService.revokeApiKey(apiKey);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<ApiKey>> getAllApiKeys() {
        return ResponseEntity.ok(apiKeyService.getAllApiKeys());
    }

    @GetMapping("/{apiKey}")
    public ResponseEntity<ApiKey> getApiKeyInfo(@PathVariable String apiKey) {
        ApiKey key = apiKeyService.getApiKeyInfo(apiKey);
        return key != null ? ResponseEntity.ok(key) : ResponseEntity.notFound().build();
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

    @GetMapping("/{apiKey}/status")
    public ResponseEntity<ApiKeyService.ApiKeyValidationResult> getApiKeyStatus(@PathVariable String apiKey) {
        ApiKeyService.ApiKeyValidationResult result = apiKeyService.validateApiKeyStatus(apiKey);
        return ResponseEntity.ok(result);
    }
} 