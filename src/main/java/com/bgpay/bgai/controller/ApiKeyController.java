package com.bgpay.bgai.controller;

import com.bgpay.bgai.entity.ApiKeyInfo;
import com.bgpay.bgai.entity.ApiKey;
import com.bgpay.bgai.entity.UserToken;
import com.bgpay.bgai.service.ApiKeyService;
import com.bgpay.bgai.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@Tag(name = "API密钥", description = "API密钥管理相关接口")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final UserService userService;

    @Operation(summary = "生成API密钥", description = "生成新的API密钥，需要JWT认证",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功生成API密钥"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
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
        ApiKeyInfo apiKey = apiKeyService.generateApiKey(clientId, clientName, description);
        return ResponseEntity.ok(apiKey);
    }

    @Operation(summary = "撤销API密钥", description = "撤销指定的API密钥")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功撤销API密钥"),
            @ApiResponse(responseCode = "404", description = "API密钥不存在")
    })
    @PostMapping("/{apiKey}/revoke")
    public ResponseEntity<Void> revokeApiKey(
            @Parameter(description = "API密钥") @PathVariable String apiKey) {
        apiKeyService.revokeApiKey(apiKey);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "获取所有API密钥", description = "获取所有API密钥列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取API密钥列表")
    })
    @GetMapping
    public ResponseEntity<List<ApiKey>> getAllApiKeys() {
        return ResponseEntity.ok(apiKeyService.getAllApiKeys());
    }

    @Operation(summary = "获取API密钥信息", description = "获取指定API密钥的详细信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取API密钥信息"),
            @ApiResponse(responseCode = "404", description = "API密钥不存在")
    })
    @GetMapping("/{apiKey}")
    public ResponseEntity<ApiKey> getApiKeyInfo(
            @Parameter(description = "API密钥") @PathVariable String apiKey) {
        ApiKey key = apiKeyService.getApiKeyInfo(apiKey);
        return key != null ? ResponseEntity.ok(key) : ResponseEntity.notFound().build();
    }

    @Operation(summary = "更新API密钥状态", description = "更新指定API密钥的启用/禁用状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功更新API密钥状态"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "404", description = "API密钥不存在")
    })
    @PutMapping("/{apiKey}/status")
    public ResponseEntity<Void> updateApiKeyStatus(
            @Parameter(description = "API密钥") @PathVariable String apiKey,
            @RequestBody Map<String, Boolean> request) {
        Boolean active = request.get("active");
        if (active != null) {
            apiKeyService.updateApiKeyStatus(apiKey, active);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(summary = "获取API密钥状态", description = "获取指定API密钥的验证状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取API密钥状态")
    })
    @GetMapping("/{apiKey}/status")
    public ResponseEntity<ApiKeyService.ApiKeyValidationResult> getApiKeyStatus(
            @Parameter(description = "API密钥") @PathVariable String apiKey) {
        ApiKeyService.ApiKeyValidationResult result = apiKeyService.validateApiKeyStatus(apiKey);
        return ResponseEntity.ok(result);
    }
} 