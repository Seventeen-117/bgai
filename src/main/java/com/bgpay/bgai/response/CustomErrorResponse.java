package com.bgpay.bgai.response;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * 自定义错误响应类
 * 用于统一API错误响应格式
 */
public class CustomErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String errorCode;
    private String message;

    public CustomErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public CustomErrorResponse(HttpStatus status, String errorCode, String message) {
        this();
        this.status = status.value();
        this.error = status.getReasonPhrase();
        this.errorCode = errorCode;
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}