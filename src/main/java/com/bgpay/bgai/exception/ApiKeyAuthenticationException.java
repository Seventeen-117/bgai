package com.bgpay.bgai.exception;

/**
 * API密钥认证异常
 * 用于API密钥认证失败的场景
 */
public class ApiKeyAuthenticationException extends RuntimeException {
    public ApiKeyAuthenticationException(String message) {
        super(message);
    }

    public ApiKeyAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
} 