package com.bgpay.bgai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 签名验证请求模型
 * 用于批量验证和异步处理
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignatureVerificationRequest {
    
    /**
     * 请求ID，用于追踪和关联结果
     */
    private String requestId;
    
    /**
     * 应用ID
     */
    private String appId;
    
    /**
     * 时间戳
     */
    private String timestamp;
    
    /**
     * 随机数
     */
    private String nonce;
    
    /**
     * 签名值
     */
    private String sign;
    
    /**
     * 请求参数
     */
    private Map<String, String> params;
    
    /**
     * 请求路径
     */
    private String path;
    
    /**
     * 客户端IP
     */
    private String clientIp;
    
    /**
     * 用户代理
     */
    private String userAgent;
    
    /**
     * 验证模式
     */
    private VerificationMode verificationMode;
    
    /**
     * 验证模式枚举
     */
    public enum VerificationMode {
        /**
         * 完全验证模式
         */
        FULL,
        
        /**
         * 快速验证模式
         */
        QUICK,
        
        /**
         * 混合验证模式
         */
        HYBRID,
        
        /**
         * 异步验证模式
         */
        ASYNC
    }
} 