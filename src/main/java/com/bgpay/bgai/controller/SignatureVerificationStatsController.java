package com.bgpay.bgai.controller;

import com.bgpay.bgai.listener.SignatureVerificationEventListener;
import com.bgpay.bgai.model.SignatureVerificationRequest;
import com.bgpay.bgai.service.SignatureVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 签名验证统计控制器
 * 提供验证统计信息和异步验证演示
 */
@Slf4j
@RestController
@RequestMapping("/api/signature-stats")
@RequiredArgsConstructor
public class SignatureVerificationStatsController {

    private final SignatureVerificationService signatureVerificationService;
    private final SignatureVerificationEventListener eventListener;

    /**
     * 获取应用验证统计信息
     */
    @GetMapping("/stats/{appId}")
    public ResponseEntity<Map<String, Object>> getAppStats(@PathVariable String appId) {
        try {
            SignatureVerificationEventListener.SignatureVerificationStats stats = eventListener.getAppStats(appId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting stats for appId: {}", appId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to get stats");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 异步验证演示
     */
    @PostMapping("/verify-async")
    public ResponseEntity<Map<String, Object>> verifyAsync(@RequestBody Map<String, Object> request) {
        try {
            String appId = (String) request.get("appId");
            String timestamp = (String) request.get("timestamp");
            String nonce = (String) request.get("nonce");
            String sign = (String) request.get("sign");
            @SuppressWarnings("unchecked")
            Map<String, String> params = (Map<String, String>) request.get("params");
            String mode = (String) request.getOrDefault("mode", "HYBRID");

            if (appId == null || timestamp == null || nonce == null || sign == null || params == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Missing required parameters");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 添加签名参数到params
            params.put("appId", appId);
            params.put("timestamp", timestamp);
            params.put("nonce", nonce);

            CompletableFuture<Boolean> verificationFuture;
            long startTime = System.currentTimeMillis();

            switch (mode.toUpperCase()) {
                case "ASYNC":
                    verificationFuture = signatureVerificationService.verifySignatureAsync(params, sign, appId);
                    break;
                case "HYBRID":
                    verificationFuture = signatureVerificationService.verifySignatureFast(params, sign, appId);
                    break;
                case "QUICK":
                    boolean quickResult = signatureVerificationService.verifySignatureQuick(params, appId);
                    Map<String, Object> quickResponse = new HashMap<>();
                    quickResponse.put("success", true);
                    quickResponse.put("mode", "QUICK");
                    quickResponse.put("result", quickResult);
                    quickResponse.put("verificationTime", System.currentTimeMillis() - startTime);
                    return ResponseEntity.ok(quickResponse);
                default:
                    verificationFuture = signatureVerificationService.verifySignatureFast(params, sign, appId);
                    break;
            }

            // 等待异步验证结果
            Boolean result = verificationFuture.get(10, TimeUnit.SECONDS);
            long verificationTime = System.currentTimeMillis() - startTime;

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mode", mode);
            response.put("result", result);
            response.put("verificationTime", verificationTime);
            response.put("appId", appId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during async verification", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Async verification failed");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 批量验证演示
     */
    @PostMapping("/verify-batch")
    public ResponseEntity<Map<String, Object>> verifyBatch(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> requests = (List<Map<String, Object>>) request.get("requests");

            if (requests == null || requests.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "No requests provided");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            List<SignatureVerificationRequest> verificationRequests = new ArrayList<>();
            long startTime = System.currentTimeMillis();

            for (Map<String, Object> req : requests) {
                String appId = (String) req.get("appId");
                String timestamp = (String) req.get("timestamp");
                String nonce = (String) req.get("nonce");
                String sign = (String) req.get("sign");
                @SuppressWarnings("unchecked")
                Map<String, String> params = (Map<String, String>) req.get("params");

                if (appId != null && timestamp != null && nonce != null && sign != null && params != null) {
                    // 添加签名参数到params
                    params.put("appId", appId);
                    params.put("timestamp", timestamp);
                    params.put("nonce", nonce);

                    SignatureVerificationRequest verificationRequest = SignatureVerificationRequest.builder()
                            .requestId(UUID.randomUUID().toString())
                            .appId(appId)
                            .timestamp(timestamp)
                            .nonce(nonce)
                            .sign(sign)
                            .params(params)
                            .verificationMode(SignatureVerificationRequest.VerificationMode.FULL)
                            .build();

                    verificationRequests.add(verificationRequest);
                }
            }

            if (verificationRequests.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "No valid requests found");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 执行批量验证
            CompletableFuture<List<Boolean>> batchFuture = signatureVerificationService.verifySignatureBatch(verificationRequests);
            List<Boolean> results = batchFuture.get(30, TimeUnit.SECONDS);
            long verificationTime = System.currentTimeMillis() - startTime;

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mode", "BATCH");
            response.put("totalRequests", verificationRequests.size());
            response.put("successCount", results.stream().filter(r -> r).count());
            response.put("failureCount", results.stream().filter(r -> !r).count());
            response.put("verificationTime", verificationTime);
            response.put("results", results);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during batch verification", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Batch verification failed");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 获取系统验证统计概览
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        try {
            Map<String, Object> overview = new HashMap<>();
            overview.put("success", true);
            overview.put("timestamp", System.currentTimeMillis());
            overview.put("message", "Signature verification system overview");
            overview.put("features", Arrays.asList(
                "Synchronous verification",
                "Asynchronous verification",
                "Hybrid verification",
                "Quick verification",
                "Batch verification",
                "Event-driven monitoring",
                "Performance metrics",
                "Security alerts"
            ));

            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            log.error("Error getting overview", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to get overview");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
} 