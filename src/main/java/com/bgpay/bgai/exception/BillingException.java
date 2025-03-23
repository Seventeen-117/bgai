package com.bgpay.bgai.exception;


public class BillingException extends RuntimeException {

    public BillingException(String message, Throwable cause) {
        super(message, cause);
    }


    public BillingException(String message) {
        super(message);
    }

}