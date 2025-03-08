package com.bgpay.bgai.entity;



// 新增请求参数接收类
public class ChatRequest {
    private String userMessage;

    // 必须有无参构造函数
    public ChatRequest() {}

    public ChatRequest(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }
}