package com.bgpay.bgai.exception;

/**
 * 计费异常
 * 用于表示计费相关错误
 */
public class BillingException extends RuntimeException {
    
    public BillingException(String message) {
        super(message);
    }
    
    public BillingException(String message, Throwable cause) {
        super(message, cause);
    }
}