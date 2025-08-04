package com.bgpay.bgai.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.bgpay.bgai.entity.AppSecret;
import com.bgpay.bgai.event.SignatureVerificationEvent;
import com.bgpay.bgai.mapper.AppSecretMapper;
import com.bgpay.bgai.model.SignatureVerificationRequest;
import com.bgpay.bgai.service.SignatureVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 签名验证服务实现类
 * 实现HMAC-SHA256签名验证、时间戳验证和nonce防重放攻击
 * 支持异步验证、批量处理和事件驱动架构
 */
@Slf4j
@Service
public class SignatureVerificationServiceImpl implements SignatureVerificationService {

    private final StringRedisTemplate redisTemplate;
    private final AppSecretMapper appSecretMapper;
    
    @Autowired
    private ApplicationContext applicationContext;

    public SignatureVerificationServiceImpl(StringRedisTemplate redisTemplate, AppSecretMapper appSecretMapper) {
        this.redisTemplate = redisTemplate;
        this.appSecretMapper = appSecretMapper;
    }

    // Redis key前缀
    private static final String NONCE_CACHE_PREFIX = "signature:nonce:";
    private static final String APP_SECRET_PREFIX = "signature:app_secret:";

    // 异步处理线程池
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(10);
    
    // 批量处理线程池
    private final ExecutorService batchExecutor = Executors.newFixedThreadPool(5);

    @Override
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

    @Override
    public boolean validateNonce(String nonce, long cacheExpireSeconds) {
        if (StrUtil.isBlank(nonce)) {
            return false;
        }

        String cacheKey = NONCE_CACHE_PREFIX + nonce;
        Boolean hasKey = redisTemplate.hasKey(cacheKey);
        
        // 如果Redis中已存在该nonce，说明已被使用过
        return hasKey == null || !hasKey;
    }

    @Override
    public void saveNonce(String nonce, long expireSeconds) {
        if (StrUtil.isNotBlank(nonce)) {
            String cacheKey = NONCE_CACHE_PREFIX + nonce;
            redisTemplate.opsForValue().set(cacheKey, "1", expireSeconds, TimeUnit.SECONDS);
            log.debug("Saved nonce to cache: {}", nonce);
        }
    }

    @Override
    public boolean verifySignature(Map<String, String> params, String sign, String appId) {
        long startTime = System.currentTimeMillis();
        boolean success = false;
        String errorMessage = null;
        
        try {
            // 获取应用密钥
            String appSecret = getAppSecret(appId);
            if (StrUtil.isBlank(appSecret)) {
                errorMessage = "App secret not found";
                log.warn("App secret not found for appId: {}", appId);
                return false;
            }

            // 构造待签名字符串
            String stringToSign = buildStringToSign(params);
            
            // 计算签名
            String calculatedSign = calculateSignature(stringToSign, appSecret);
            
            // 比较签名
            success = StrUtil.equalsIgnoreCase(sign, calculatedSign);
            
            if (!success) {
                errorMessage = "Signature verification failed";
                log.warn("Signature verification failed for appId: {}, expected: {}, actual: {}", 
                        appId, calculatedSign, sign);
            }
            
            return success;
        } catch (Exception e) {
            errorMessage = "System error during signature verification";
            log.error("Error during signature verification for appId: {}", appId, e);
            return false;
        } finally {
            // 发布验证事件
            publishVerificationEvent(appId, null, null, params, success, errorMessage, 
                    System.currentTimeMillis() - startTime);
        }
    }

    // ========== 异步验证方法实现 ==========

    @Override
    public CompletableFuture<Boolean> verifySignatureAsync(Map<String, String> params, String sign, String appId) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            boolean success = false;
            String errorMessage = null;
            
            try {
                // 获取应用密钥
                String appSecret = getAppSecret(appId);
                if (StrUtil.isBlank(appSecret)) {
                    errorMessage = "App secret not found";
                    log.warn("App secret not found for appId: {}", appId);
                    return false;
                }

                // 构造待签名字符串
                String stringToSign = buildStringToSign(params);
                
                // 计算签名
                String calculatedSign = calculateSignature(stringToSign, appSecret);
                
                // 比较签名
                success = StrUtil.equalsIgnoreCase(sign, calculatedSign);
                
                if (!success) {
                    errorMessage = "Signature verification failed";
                    log.warn("Signature verification failed for appId: {}, expected: {}, actual: {}", 
                            appId, calculatedSign, sign);
                }
                
                return success;
            } catch (Exception e) {
                errorMessage = "System error during signature verification";
                log.error("Error during signature verification for appId: {}", appId, e);
                return false;
            } finally {
                // 发布验证事件
                publishVerificationEvent(appId, null, null, params, success, errorMessage, 
                        System.currentTimeMillis() - startTime);
            }
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Boolean> verifySignatureFast(Map<String, String> params, String sign, String appId) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            boolean success = false;
            String errorMessage = null;
            
            try {
                // 快速基础验证：只验证参数完整性和时间戳
                String timestamp = params.get("timestamp");
                if (StrUtil.isBlank(timestamp) || !validateTimestamp(timestamp, 300)) {
                    errorMessage = "Timestamp validation failed";
                    return false;
                }

                // 异步详细验证：在后台执行完整的签名验证流程
                return verifySignature(params, sign, appId);
            } catch (Exception e) {
                errorMessage = "System error during fast signature verification";
                log.error("Error during fast signature verification for appId: {}", appId, e);
                return false;
            } finally {
                // 发布验证事件
                publishVerificationEvent(appId, null, null, params, success, errorMessage, 
                        System.currentTimeMillis() - startTime);
            }
        }, asyncExecutor);
    }

    @Override
    public boolean verifySignatureQuick(Map<String, String> params, String appId) {
        // 仅进行最基础的参数检查和时间戳验证
        // 跳过耗时的nonce和签名验证
        
        // 检查必需参数
        String timestamp = params.get("timestamp");
        if (StrUtil.isBlank(timestamp)) {
            return false;
        }
        
        // 简化时间戳验证
        try {
            long timestampValue = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis();
            long expireTime = 300 * 1000; // 5分钟
            
            return Math.abs(currentTime - timestampValue) <= expireTime;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public CompletableFuture<List<Boolean>> verifySignatureBatch(List<SignatureVerificationRequest> verificationRequests) {
        return CompletableFuture.supplyAsync(() -> {
            // 按客户端ID分组，便于批量获取密钥和nonce验证
            Map<String, List<SignatureVerificationRequest>> groupedRequests = verificationRequests.stream()
                    .collect(Collectors.groupingBy(SignatureVerificationRequest::getAppId));
            
            List<Boolean> results = new ArrayList<>();
            
            // 并行处理每个分组
            groupedRequests.forEach((appId, requests) -> {
                // 批量预加载客户端密钥
                String appSecret = getAppSecret(appId);
                
                // 利用Java 8 Stream的并行流特性，充分利用多核CPU
                List<Boolean> groupResults = requests.parallelStream()
                        .map(request -> {
                            try {
                                return verifySignature(request.getParams(), request.getSign(), appId);
                            } catch (Exception e) {
                                log.error("Error during batch verification for request: {}", request.getRequestId(), e);
                                return false;
                            }
                        })
                        .collect(Collectors.toList());
                
                results.addAll(groupResults);
            });
            
            return results;
        }, batchExecutor);
    }

    @Override
    public CompletableFuture<Void> saveNonceAsync(String nonce, long expireSeconds) {
        return CompletableFuture.runAsync(() -> {
            if (StrUtil.isNotBlank(nonce)) {
                String cacheKey = NONCE_CACHE_PREFIX + nonce;
                redisTemplate.opsForValue().set(cacheKey, "1", expireSeconds, TimeUnit.SECONDS);
                log.debug("Saved nonce to cache: {}", nonce);
            }
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Boolean> validateNonceAsync(String nonce, long cacheExpireSeconds) {
        return CompletableFuture.supplyAsync(() -> {
            if (StrUtil.isBlank(nonce)) {
                return false;
            }

            String cacheKey = NONCE_CACHE_PREFIX + nonce;
            Boolean hasKey = redisTemplate.hasKey(cacheKey);
            
            return hasKey == null || !hasKey;
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Boolean> validateTimestampAsync(String timestamp, long expireSeconds) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                long timestampValue = Long.parseLong(timestamp);
                long currentTime = System.currentTimeMillis();
                long expireTime = expireSeconds * 1000;
                
                return Math.abs(currentTime - timestampValue) <= expireTime;
            } catch (NumberFormatException e) {
                log.warn("Invalid timestamp format: {}", timestamp);
                return false;
            }
        }, asyncExecutor);
    }

    // ========== 私有辅助方法 ==========

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
     * 发布验证事件
     * 用于事件驱动架构，支持监控、告警和审计
     * 
     * @param appId 应用ID
     * @param path 请求路径
     * @param clientIp 客户端IP
     * @param params 请求参数
     * @param success 验证结果
     * @param errorMessage 错误信息
     * @param verificationTime 验证耗时
     */
    private void publishVerificationEvent(String appId, String path, String clientIp, 
                                        Map<String, String> params, boolean success, 
                                        String errorMessage, long verificationTime) {
        try {
            SignatureVerificationEvent.EventType eventType = success ? 
                    SignatureVerificationEvent.EventType.VERIFICATION_SUCCESS :
                    SignatureVerificationEvent.EventType.VERIFICATION_FAILED;
            
            SignatureVerificationEvent event = SignatureVerificationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(eventType)
                    .appId(appId)
                    .path(path)
                    .clientIp(clientIp)
                    .params(params)
                    .success(success)
                    .errorMessage(errorMessage)
                    .verificationTime(verificationTime)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            applicationContext.publishEvent(event);
        } catch (Exception e) {
            log.warn("Failed to publish verification event", e);
        }
    }

    @Override
    public String generateNonce() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public String generateTimestamp() {
        return String.valueOf(System.currentTimeMillis());
    }
} 