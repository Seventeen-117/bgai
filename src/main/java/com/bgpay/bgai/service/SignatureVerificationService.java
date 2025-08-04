package com.bgpay.bgai.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.bgpay.bgai.entity.AppSecret;
import com.bgpay.bgai.mapper.AppSecretMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * 签名验证服务
 * 实现HMAC-SHA256签名验证、时间戳验证和nonce防重放攻击
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignatureVerificationService {

    private final StringRedisTemplate redisTemplate;
    private final ApiKeyService apiKeyService;
    private final AppSecretMapper appSecretMapper;

    // Redis key前缀
    private static final String NONCE_CACHE_PREFIX = "signature:nonce:";
    private static final String APP_SECRET_PREFIX = "signature:app_secret:";

    /**
     * 验证时间戳是否在有效期内
     * 
     * @param timestamp 时间戳（毫秒）
     * @param expireSeconds 有效期（秒）
     * @return 是否有效
     */
    public boolean validateTimestamp(String timestamp, long expireSeconds) {
        try {
            long timestampValue = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis();
            long expireTime = expireSeconds * 1000; // 转换为毫秒
            
            // 检查时间戳是否在有效期内
            return Math.abs(currentTime - timestampValue) <= expireTime;
        } catch (NumberFormatException e) {
            log.warn("Invalid timestamp format: {}", timestamp);
            return false;
        }
    }

    /**
     * 验证nonce是否已被使用（防重放攻击）
     * 
     * @param nonce 随机数
     * @param cacheExpireSeconds 缓存过期时间（秒）
     * @return 是否有效（true表示未使用过，false表示已使用过）
     */
    public boolean validateNonce(String nonce, long cacheExpireSeconds) {
        if (StrUtil.isBlank(nonce)) {
            return false;
        }

        String cacheKey = NONCE_CACHE_PREFIX + nonce;
        Boolean hasKey = redisTemplate.hasKey(cacheKey);
        
        // 如果Redis中已存在该nonce，说明已被使用过
        return hasKey == null || !hasKey;
    }

    /**
     * 保存nonce到缓存
     * 
     * @param nonce 随机数
     * @param expireSeconds 过期时间（秒）
     */
    public void saveNonce(String nonce, long expireSeconds) {
        if (StrUtil.isNotBlank(nonce)) {
            String cacheKey = NONCE_CACHE_PREFIX + nonce;
            redisTemplate.opsForValue().set(cacheKey, "1", expireSeconds, TimeUnit.SECONDS);
            log.debug("Saved nonce to cache: {}", nonce);
        }
    }

    /**
     * 验证签名
     * 
     * @param params 请求参数
     * @param sign 签名值
     * @param appId 应用ID
     * @return 是否验证通过
     */
    public boolean verifySignature(Map<String, String> params, String sign, String appId) {
        try {
            // 获取应用密钥
            String appSecret = getAppSecret(appId);
            if (StrUtil.isBlank(appSecret)) {
                log.warn("App secret not found for appId: {}", appId);
                return false;
            }

            // 构造待签名字符串
            String stringToSign = buildStringToSign(params);
            
            // 计算签名
            String calculatedSign = calculateSignature(stringToSign, appSecret);
            
            // 比较签名
            boolean isValid = StrUtil.equalsIgnoreCase(sign, calculatedSign);
            
            if (!isValid) {
                log.warn("Signature verification failed for appId: {}, expected: {}, actual: {}", 
                        appId, calculatedSign, sign);
            }
            
            return isValid;
        } catch (Exception e) {
            log.error("Error during signature verification for appId: {}", appId, e);
            return false;
        }
    }

    /**
     * 构造待签名字符串
     * 按照字典序排列参数，拼接成key=value&格式
     * 
     * @param params 请求参数
     * @return 待签名字符串
     */
    private String buildStringToSign(Map<String, String> params) {
        // 创建排序的Map
        TreeMap<String, String> sortedParams = new TreeMap<>();
        
        // 过滤掉sign参数，只保留其他参数
        params.forEach((key, value) -> {
            if (!"sign".equals(key) && StrUtil.isNotBlank(value)) {
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
    private String calculateSignature(String stringToSign, String secret) {
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
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 获取应用密钥
     * 这里可以根据实际需求从数据库或缓存中获取
     * 
     * @param appId 应用ID
     * @return 应用密钥
     */
    private String getAppSecret(String appId) {
        // 先从Redis缓存中获取
        String cacheKey = APP_SECRET_PREFIX + appId;
        String cachedSecret = redisTemplate.opsForValue().get(cacheKey);
        
        if (StrUtil.isNotBlank(cachedSecret)) {
            return cachedSecret;
        }
        
        // 如果缓存中没有，从数据库获取
        // 这里可以根据实际的数据库结构来实现
        // 暂时使用一个简单的映射关系
        String appSecret = getAppSecretFromDatabase(appId);
        
        if (StrUtil.isNotBlank(appSecret)) {
            // 缓存到Redis，过期时间设置为1小时
            redisTemplate.opsForValue().set(cacheKey, appSecret, 1, TimeUnit.HOURS);
        }
        
        return appSecret;
    }

    /**
     * 从数据库获取应用密钥
     * 
     * @param appId 应用ID
     * @return 应用密钥
     */
    private String getAppSecretFromDatabase(String appId) {
        try {
            AppSecret appSecret = appSecretMapper.selectByAppId(appId);
            if (appSecret != null && appSecret.getStatus() == 1) {
                return appSecret.getAppSecret();
            }
        } catch (Exception e) {
            log.warn("Failed to get app secret from database for appId: {}", appId, e);
        }
        
        // 如果数据库中没有找到，返回null
        return null;
    }

    /**
     * 生成nonce（随机数）
     * 
     * @return 随机数
     */
    public String generateNonce() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成时间戳
     * 
     * @return 当前时间戳（毫秒）
     */
    public String generateTimestamp() {
        return String.valueOf(System.currentTimeMillis());
    }
} 