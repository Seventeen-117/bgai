package com.bgpay.bgai.utils;

import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 签名工具类
 * 提供客户端生成签名的功能
 */
@Slf4j
public class SignatureUtils {

    /**
     * 生成nonce（随机数）
     * 
     * @return 随机数
     */
    public static String generateNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成时间戳
     * 
     * @return 当前时间戳（毫秒）
     */
    public static String generateTimestamp() {
        return String.valueOf(System.currentTimeMillis());
    }

    /**
     * 构造待签名字符串
     * 按照字典序排列参数，拼接成key=value&格式
     * 
     * @param params 请求参数
     * @return 待签名字符串
     */
    public static String buildStringToSign(Map<String, String> params) {
        // 创建排序的Map
        TreeMap<String, String> sortedParams = new TreeMap<>();
        
        // 过滤掉sign参数，只保留其他参数
        params.forEach((key, value) -> {
            if (!"sign".equals(key) && value != null && !value.isEmpty()) {
                sortedParams.put(key, value);
            }
        });
        
        // 构造key=value&格式的字符串
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        
        return sb.toString();
    }

    /**
     * 计算HMAC-SHA256签名
     * 
     * @param stringToSign 待签名字符串
     * @param secret 密钥
     * @return 签名值（十六进制字符串）
     */
    public static String calculateSignature(String stringToSign, String secret) {
        HMac hMac = new HMac(HmacAlgorithm.HmacSHA256, secret.getBytes(StandardCharsets.UTF_8));
        byte[] digest = hMac.digest(stringToSign.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(digest);
    }

    /**
     * 字节数组转十六进制字符串
     * 
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 生成完整的签名参数
     * 
     * @param appId 应用ID
     * @param secret 应用密钥
     * @param businessParams 业务参数
     * @return 包含签名参数的Map
     */
    public static Map<String, String> generateSignatureParams(String appId, String secret, Map<String, String> businessParams) {
        Map<String, String> allParams = new TreeMap<>();
        
        // 添加业务参数
        if (businessParams != null) {
            allParams.putAll(businessParams);
        }
        
        // 添加签名必需参数
        allParams.put("appId", appId);
        allParams.put("timestamp", generateTimestamp());
        allParams.put("nonce", generateNonce());
        
        // 构造待签名字符串
        String stringToSign = buildStringToSign(allParams);
        
        // 计算签名
        String sign = calculateSignature(stringToSign, secret);
        
        // 添加签名
        allParams.put("sign", sign);
        
        return allParams;
    }

    /**
     * 验证签名
     * 
     * @param params 请求参数
     * @param secret 密钥
     * @return 是否验证通过
     */
    public static boolean verifySignature(Map<String, String> params, String secret) {
        try {
            String sign = params.get("sign");
            if (sign == null || sign.isEmpty()) {
                return false;
            }
            
            // 构造待签名字符串
            String stringToSign = buildStringToSign(params);
            
            // 计算签名
            String calculatedSign = calculateSignature(stringToSign, secret);
            
            // 比较签名
            return sign.equalsIgnoreCase(calculatedSign);
        } catch (Exception e) {
            log.error("Error during signature verification", e);
            return false;
        }
    }

    /**
     * 生成示例签名参数（用于测试）
     * 
     * @param appId 应用ID
     * @param secret 应用密钥
     * @return 示例参数
     */
    public static Map<String, String> generateExampleParams(String appId, String secret) {
        Map<String, String> businessParams = new TreeMap<>();
        businessParams.put("param1", "value1");
        businessParams.put("param2", "value2");
        
        return generateSignatureParams(appId, secret, businessParams);
    }
} 