package com.bgpay.bgai.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 签名验证事件
 * 用于事件驱动架构，支持监控、告警和审计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignatureVerificationEvent {
    
    /**
     * 事件ID
     */
    private String eventId;
    
    /**
     * 事件类型
     */
    private EventType eventType;
    
    /**
     * 应用ID
     */
    private String appId;
    
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
     * 请求参数
     */
    private Map<String, String> params;
    
    /**
     * 验证结果
     */
    private boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 验证耗时（毫秒）
     */
    private long verificationTime;
    
    /**
     * 事件发生时间
     */
    private LocalDateTime timestamp;
    
    /**
     * 事件类型枚举
     */
    public enum EventType {
        /**
         * 验证成功
         */
        VERIFICATION_SUCCESS,
        
        /**
         * 验证失败
         */
        VERIFICATION_FAILED,
        
        /**
         * 重放攻击检测
         */
        REPLAY_ATTACK_DETECTED,
        
        /**
         * 时间戳过期
         */
        TIMESTAMP_EXPIRED,
        
        /**
         * 签名不匹配
         */
        SIGNATURE_MISMATCH,
        
        /**
         * 参数缺失
         */
        MISSING_PARAMETERS,
        
        /**
         * 应用密钥未找到
         */
        APP_SECRET_NOT_FOUND,
        
        /**
         * 系统错误
         */
        SYSTEM_ERROR
    }
} 