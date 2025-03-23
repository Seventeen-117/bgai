package com.bgpay.bgai.service.deepseek;

import com.bgpay.bgai.response.ChatResponse;
import reactor.core.publisher.Mono;

public interface DeepSeekService {
    public ChatResponse processRequest(String content,
                                       String apiUrl,
                                       String apiKey,
                                       String modelName,
                                       String userId,
                                       boolean multiTurn);

    public Mono<ChatResponse> processRequestReactive(String content,
                                                     String apiUrl,
                                                     String apiKey,
                                                     String modelName,
                                                     String userId,
                                                     boolean multiTurn);
}