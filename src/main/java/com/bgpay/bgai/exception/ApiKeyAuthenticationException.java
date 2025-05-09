package com.bgpay.bgai.exception;

public class ApiKeyAuthenticationException extends RuntimeException {
    public ApiKeyAuthenticationException(String message) {
        super(message);
    }

    public ApiKeyAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
} 