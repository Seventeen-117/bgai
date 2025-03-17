package com.bgpay.bgai.service.deepseek;

import com.bgpay.bgai.response.ChatResponse;

public interface DeepSeekService {
    public ChatResponse processRequest(String content, String apiUrl, String apiKey, String modelName, String userId);
}