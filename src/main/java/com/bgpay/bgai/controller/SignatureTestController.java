package com.bgpay.bgai.controller;

import com.bgpay.bgai.utils.SignatureUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 签名验证测试控制器
 * 提供签名验证的测试接口和示例
 */
@Slf4j
@RestController
@RequestMapping("/api/signature")
@RequiredArgsConstructor
public class SignatureTestController {

    /**
     * 测试签名验证的接口
     * 需要提供appId、timestamp、nonce、sign等签名参数
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testSignature(
            @RequestParam String appId,
            @RequestParam String timestamp,
            @RequestParam String nonce,
            @RequestParam String sign,
            @RequestParam(required = false) String param1,
            @RequestParam(required = false) String param2) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Signature verification passed");
        response.put("appId", appId);
        response.put("timestamp", timestamp);
        response.put("nonce", nonce);
        response.put("receivedParams", Map.of(
                "param1", param1,
                "param2", param2
        ));
        response.put("serverTime", System.currentTimeMillis());
        
        log.info("Signature test passed for appId: {}", appId);
        return ResponseEntity.ok(response);
    }

    /**
     * 生成签名示例
     * 返回用于测试的签名参数
     */
    @GetMapping("/generate-example")
    public ResponseEntity<Map<String, Object>> generateSignatureExample(
            @RequestParam(defaultValue = "test-app-001") String appId,
            @RequestParam(defaultValue = "secret_test_app_001") String secret) {
        
        // 生成示例业务参数
        Map<String, String> businessParams = new HashMap<>();
        businessParams.put("param1", "value1");
        businessParams.put("param2", "value2");
        businessParams.put("action", "test");
        
        // 生成签名参数
        Map<String, String> signatureParams = SignatureUtils.generateSignatureParams(appId, secret, businessParams);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Signature parameters generated successfully");
        response.put("appId", appId);
        response.put("secret", secret);
        response.put("signatureParams", signatureParams);
        response.put("stringToSign", SignatureUtils.buildStringToSign(signatureParams));
        response.put("usage", "Use these parameters to test the signature verification endpoint");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 验证签名示例
     * 验证提供的签名参数是否正确
     */
    @PostMapping("/verify-example")
    public ResponseEntity<Map<String, Object>> verifySignatureExample(
            @RequestParam String appId,
            @RequestParam String timestamp,
            @RequestParam String nonce,
            @RequestParam String sign,
            @RequestParam(defaultValue = "secret_test_app_001") String secret,
            @RequestParam(required = false) String param1,
            @RequestParam(required = false) String param2) {
        
        // 构造参数Map
        Map<String, String> params = new HashMap<>();
        params.put("appId", appId);
        params.put("timestamp", timestamp);
        params.put("nonce", nonce);
        params.put("sign", sign);
        if (param1 != null) params.put("param1", param1);
        if (param2 != null) params.put("param2", param2);
        
        // 验证签名
        boolean isValid = SignatureUtils.verifySignature(params, secret);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", isValid);
        response.put("message", isValid ? "Signature verification passed" : "Signature verification failed");
        response.put("appId", appId);
        response.put("timestamp", timestamp);
        response.put("nonce", nonce);
        response.put("stringToSign", SignatureUtils.buildStringToSign(params));
        response.put("expectedSign", SignatureUtils.calculateSignature(SignatureUtils.buildStringToSign(params), secret));
        response.put("receivedSign", sign);
        
        if (isValid) {
            log.info("Signature verification example passed for appId: {}", appId);
        } else {
            log.warn("Signature verification example failed for appId: {}", appId);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取签名算法说明
     */
    @GetMapping("/algorithm-info")
    public ResponseEntity<Map<String, Object>> getAlgorithmInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("algorithm", "HMAC-SHA256");
        response.put("description", "接口签名验证算法说明");
        response.put("steps", new String[]{
                "1. 准备请求参数（包括业务参数和签名参数）",
                "2. 生成当前时间戳（timestamp，毫秒）",
                "3. 生成唯一随机数（nonce）",
                "4. 按字典序排列所有参数（除sign外）",
                "5. 构造待签名字符串：key1=value1&key2=value2",
                "6. 使用HMAC-SHA256算法和密钥计算签名",
                "7. 将签名值转换为十六进制字符串",
                "8. 将签名值作为sign参数添加到请求中"
        });
        response.put("requiredParams", new String[]{
                "appId - 应用标识",
                "timestamp - 时间戳（毫秒）",
                "nonce - 随机数",
                "sign - 签名值"
        });
        response.put("securityFeatures", new String[]{
                "时间戳验证 - 防止过期请求（默认5分钟）",
                "Nonce验证 - 防止重放攻击（缓存30分钟）",
                "签名验证 - 验证请求完整性和来源可信性"
        });
        
        return ResponseEntity.ok(response);
    }
} 