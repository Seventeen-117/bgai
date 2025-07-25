package com.bgpay.bgai.exception;

public class FlinkJobException extends Exception {
    public FlinkJobException(String msg) { super(msg); }
    public FlinkJobException(String msg, Throwable cause) { super(msg, cause); }
} 