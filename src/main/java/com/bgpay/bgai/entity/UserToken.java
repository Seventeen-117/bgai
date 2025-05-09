package com.bgpay.bgai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户令牌实体，用于缓存和序列化
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserToken implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户唯一标识
     */
    private String userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 用户邮箱
     */
    private String email;
    
    /**
     * SSO 访问令牌
     */
    private String accessToken;
    
    /**
     * 令牌过期时间
     */
    private LocalDateTime tokenExpireTime;
    
    /**
     * 登录时间
     */
    private LocalDateTime loginTime;
    
    /**
     * 令牌是否有效
     */
    private boolean valid;
} 