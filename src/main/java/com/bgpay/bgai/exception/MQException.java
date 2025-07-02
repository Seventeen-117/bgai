package com.bgpay.bgai.exception;

/**
 * 消息队列异常
 * 用于表示消息队列操作中的错误
 */
public class MQException extends RuntimeException {
    
    public MQException(String message) {
        super(message);
    }
    
    public MQException(String message, Throwable cause) {
        super(message, cause);
    }
}
