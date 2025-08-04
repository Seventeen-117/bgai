package com.bgpay.bgai;

import com.bgpay.bgai.utils.SignatureUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * 签名验证功能测试
 */
public class SignatureVerificationTest {

    @Test
    public void testGenerateNonce() {
        String nonce1 = SignatureUtils.generateNonce();
        String nonce2 = SignatureUtils.generateNonce();
        
        Assert.assertNotNull(nonce1);
        Assert.assertNotNull(nonce2);
        Assert.assertNotEquals(nonce1, nonce2);
        Assert.assertEquals(nonce1.length(), 32);
        Assert.assertEquals(nonce2.length(), 32);
    }

    @Test
    public void testGenerateTimestamp() {
        String timestamp = SignatureUtils.generateTimestamp();
        
        Assert.assertNotNull(timestamp);
        Assert.assertTrue(timestamp.matches("\\d+"));
        
        long timestampValue = Long.parseLong(timestamp);
        long currentTime = System.currentTimeMillis();
        
        // 时间戳应该在当前时间的合理范围内（前后1分钟）
        Assert.assertTrue(Math.abs(currentTime - timestampValue) < 60000);
    }

    @Test
    public void testBuildStringToSign() {
        Map<String, String> params = new HashMap<>();
        params.put("appId", "test-app");
        params.put("timestamp", "1703123456789");
        params.put("nonce", "abc123def456");
        params.put("param1", "value1");
        params.put("param2", "value2");
        params.put("sign", "should-be-excluded");
        
        String stringToSign = SignatureUtils.buildStringToSign(params);
        
        // 应该按字典序排列，不包含sign参数
        Assert.assertEquals(stringToSign, "appId=test-app&nonce=abc123def456&param1=value1&param2=value2&timestamp=1703123456789");
    }

    @Test
    public void testCalculateSignature() {
        String stringToSign = "appId=test-app&nonce=abc123def456&param1=value1&param2=value2&timestamp=1703123456789";
        String secret = "test-secret";
        
        String signature = SignatureUtils.calculateSignature(stringToSign, secret);
        
        Assert.assertNotNull(signature);
        Assert.assertTrue(signature.matches("[a-f0-9]{64}")); // 64位十六进制字符串
    }

    @Test
    public void testGenerateSignatureParams() {
        Map<String, String> businessParams = new HashMap<>();
        businessParams.put("param1", "value1");
        businessParams.put("param2", "value2");
        
        Map<String, String> signatureParams = SignatureUtils.generateSignatureParams(
            "test-app-001", 
            "secret_test_app_001", 
            businessParams
        );
        
        Assert.assertNotNull(signatureParams);
        Assert.assertEquals(signatureParams.get("appId"), "test-app-001");
        Assert.assertNotNull(signatureParams.get("timestamp"));
        Assert.assertNotNull(signatureParams.get("nonce"));
        Assert.assertNotNull(signatureParams.get("sign"));
        Assert.assertEquals(signatureParams.get("param1"), "value1");
        Assert.assertEquals(signatureParams.get("param2"), "value2");
    }

    @Test
    public void testVerifySignature() {
        // 生成签名参数
        Map<String, String> businessParams = new HashMap<>();
        businessParams.put("param1", "value1");
        businessParams.put("param2", "value2");
        
        Map<String, String> signatureParams = SignatureUtils.generateSignatureParams(
            "test-app-001", 
            "secret_test_app_001", 
            businessParams
        );
        
        // 验证签名
        boolean isValid = SignatureUtils.verifySignature(signatureParams, "secret_test_app_001");
        Assert.assertTrue(isValid);
        
        // 使用错误密钥验证
        boolean isInvalid = SignatureUtils.verifySignature(signatureParams, "wrong-secret");
        Assert.assertFalse(isInvalid);
        
        // 修改参数后验证
        signatureParams.put("param1", "modified-value");
        boolean isModifiedInvalid = SignatureUtils.verifySignature(signatureParams, "secret_test_app_001");
        Assert.assertFalse(isModifiedInvalid);
    }

    @Test
    public void testVerifySignatureWithEmptySign() {
        Map<String, String> params = new HashMap<>();
        params.put("appId", "test-app");
        params.put("timestamp", "1703123456789");
        params.put("nonce", "abc123def456");
        params.put("param1", "value1");
        // 不包含sign参数
        
        boolean isValid = SignatureUtils.verifySignature(params, "test-secret");
        Assert.assertFalse(isValid);
    }

    @Test
    public void testVerifySignatureWithNullSign() {
        Map<String, String> params = new HashMap<>();
        params.put("appId", "test-app");
        params.put("timestamp", "1703123456789");
        params.put("nonce", "abc123def456");
        params.put("param1", "value1");
        params.put("sign", null);
        
        boolean isValid = SignatureUtils.verifySignature(params, "test-secret");
        Assert.assertFalse(isValid);
    }

    @Test
    public void testVerifySignatureWithEmptyStringSign() {
        Map<String, String> params = new HashMap<>();
        params.put("appId", "test-app");
        params.put("timestamp", "1703123456789");
        params.put("nonce", "abc123def456");
        params.put("param1", "value1");
        params.put("sign", "");
        
        boolean isValid = SignatureUtils.verifySignature(params, "test-secret");
        Assert.assertFalse(isValid);
    }
} 