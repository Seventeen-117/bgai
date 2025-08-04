package com.bgpay.bgai.service;

import java.util.Map;

/**
 * 签名验证服务接口
 * 定义HMAC-SHA256签名验证、时间戳验证和nonce防重放攻击的方法
 */
public interface SignatureVerificationService {

    /**
     * 验证时间戳是否在有效期内
     * 
     * @param timestamp 时间戳（毫秒）
     * @param expireSeconds 有效期（秒）
     * @return 是否有效
     */
    boolean validateTimestamp(String timestamp, long expireSeconds);

    /**
     * 验证nonce是否已被使用（防重放攻击）
     * 
     * @param nonce 随机数
     * @param cacheExpireSeconds 缓存过期时间（秒）
     * @return 是否有效（true表示未使用过，false表示已使用过）
     */
    boolean validateNonce(String nonce, long cacheExpireSeconds);

    /**
     * 保存nonce到缓存
     * 
     * @param nonce 随机数
     * @param expireSeconds 过期时间（秒）
     */
    void saveNonce(String nonce, long expireSeconds);

    /**
     * 验证签名
     * 
     * @param params 请求参数
     * @param sign 签名值
     * @param appId 应用ID
     * @return 是否验证通过
     */
    boolean verifySignature(Map<String, String> params, String sign, String appId);

    /**
     * 生成nonce（随机数）
     * 
     * @return 随机数
     */
    String generateNonce();

    /**
     * 生成时间戳
     * 
     * @return 当前时间戳（毫秒）
     */
    String generateTimestamp();
} 