package com.bgpay.bgai.service.deepseek;

import com.bgpay.bgai.response.ChatResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface DeepSeekService {
     ChatResponse processRequest(String content,
                                       String apiUrl,
                                       String apiKey,
                                       String modelName,
                                       String userId,
                                       boolean multiTurn);

     Mono<ChatResponse> processRequestReactive(String content,
                                                     String apiUrl,
                                                     String apiKey,
                                                     String modelName,
                                                     String userId,
                                                     boolean multiTurn);
                                                     
     /**
      * 处理Map格式的请求体，兼容OpenAI格式的API请求
      */
     Mono<ChatResponse> processRequestReactive(Map<String, Object> requestBody,
                                               String apiUrl,
                                               String apiKey,
                                               String modelName,
                                               String userId,
                                               boolean multiTurn);
}