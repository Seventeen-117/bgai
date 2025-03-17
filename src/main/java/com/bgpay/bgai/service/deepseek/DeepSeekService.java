package com.bgpay.bgai.deepseek;

import com.bgpay.bgai.response.ChatResponse;

public interface DeepSeekService {
    ChatResponse processRequest(String content, String apiUrl,
                                String apiKey, String modelName);
}