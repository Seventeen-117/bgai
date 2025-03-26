package com.bgpay.bgai.service.mq;


public interface MQCallback {
    void onSuccess(String messageId);
    void onFailure(String messageId, Throwable e);
}