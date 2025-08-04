package com.bgpay.bgai.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import com.bgpay.bgai.model.SignatureVerificationRequest;

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

    // ========== 异步验证方法 ==========

    /**
     * 完全异步签名验证
     * 使用CompletableFuture实现完全异步处理
     * 
     * @param params 请求参数
     * @param sign 签名值
     * @param appId 应用ID
     * @return CompletableFuture<Boolean> 异步验证结果
     */
    CompletableFuture<Boolean> verifySignatureAsync(Map<String, String> params, String sign, String appId);

    /**
     * 混合验证模式（快速基础验证 + 异步详细验证）
     * 快速基础验证：只验证参数完整性和时间戳
     * 异步详细验证：在后台执行完整的签名验证流程
     * 
     * @param params 请求参数
     * @param sign 签名值
     * @param appId 应用ID
     * @return CompletableFuture<Boolean> 异步验证结果
     */
    CompletableFuture<Boolean> verifySignatureFast(Map<String, String> params, String sign, String appId);

    /**
     * 快速验证策略
     * 仅进行最基础的参数检查和时间戳验证
     * 跳过耗时的nonce和签名验证
     * 
     * @param params 请求参数
     * @param appId 应用ID
     * @return 是否通过快速验证
     */
    boolean verifySignatureQuick(Map<String, String> params, String appId);

    /**
     * 批量验证优化
     * 在面对大量并发请求时，批量验证优化是提升系统处理能力的关键技术
     * 
     * @param verificationRequests 批量验证请求列表
     * @return CompletableFuture<List<Boolean>> 批量验证结果
     */
    CompletableFuture<List<Boolean>> verifySignatureBatch(List<SignatureVerificationRequest> verificationRequests);

    /**
     * 异步保存nonce到缓存
     * 
     * @param nonce 随机数
     * @param expireSeconds 过期时间（秒）
     * @return CompletableFuture<Void> 异步保存结果
     */
    CompletableFuture<Void> saveNonceAsync(String nonce, long expireSeconds);

    /**
     * 异步验证nonce
     * 
     * @param nonce 随机数
     * @param cacheExpireSeconds 缓存过期时间（秒）
     * @return CompletableFuture<Boolean> 异步验证结果
     */
    CompletableFuture<Boolean> validateNonceAsync(String nonce, long cacheExpireSeconds);

    /**
     * 异步验证时间戳
     * 
     * @param timestamp 时间戳（毫秒）
     * @param expireSeconds 有效期（秒）
     * @return CompletableFuture<Boolean> 异步验证结果
     */
    CompletableFuture<Boolean> validateTimestampAsync(String timestamp, long expireSeconds);
} 