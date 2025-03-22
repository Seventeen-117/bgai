package com.bgpay.bgai.exception;

// 自定义的计费异常类，继承自 RuntimeException
public class BillingException extends RuntimeException {
    // 构造方法，接收异常信息和原始异常
    public BillingException(String message, Throwable cause) {
        super(message, cause);
    }

    // 构造方法，只接收异常信息
    public BillingException(String message) {
        super(message);
    }

}