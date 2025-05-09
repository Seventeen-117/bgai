package com.bgpay.bgai.controller;

import com.bgpay.bgai.response.SimpleChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 简化版的聊天控制器，提供更精简的API响应
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class SimpleChatController {

    private final ReactiveChatController reactiveChatController;
    
    @Autowired
    public SimpleChatController(ReactiveChatController reactiveChatController) {
        this.reactiveChatController = reactiveChatController;
    }

    /**
     * 处理聊天请求，返回简化版响应
     * 使用与原接口相同的路径，但返回更简洁的响应格式
     */
    @PostMapping(
            value = "/chatGatWay",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<SimpleChatResponse>> handleSimpleChatRequest(
            @RequestPart(value = "file", required = false) FilePart file,
            @RequestParam(value = "question", defaultValue = "") String question,
            @RequestParam(value = "apiUrl", required = false) String apiUrl,
            @RequestParam(value = "apiKey", required = false) String apiKey,
            @RequestParam(value = "modelName", required = false) String modelName,
            @RequestParam(value = "multiTurn", defaultValue = "false") boolean multiTurn,
            ServerWebExchange exchange) {
        
        log.info("Received chatGatWay request: question length = {}, file = {}, modelName = {}", 
                question.length(), file != null ? file.filename() : "none", modelName);
        
        // 记录模型参数传递过程，用于调试
        exchange.getAttributes().put("requestedModelName", modelName);
        
        // 复用ReactiveChatController的handleChatRequest方法处理请求
        return reactiveChatController.handleChatRequest(
                    file, question, apiUrl, apiKey, modelName, String.valueOf(multiTurn), exchange
                )
                .map(ResponseEntity::getBody)
                .map(response -> {
                    // 记录最终使用的模型名称
                    if (response != null && response.getUsage() != null) {
                        String actualModelName = response.getUsage().getModelType();
                        log.info("Model name transformation: requested='{}', actual='{}'", 
                                modelName, actualModelName);
                    }
                    return response;
                })
                .map(SimpleChatResponse::fromChatResponse)
                .map(response -> {
                    // 如果有错误，设置对应的HTTP状态码
                    if (response.getError() != null) {
                        return ResponseEntity.status(response.getError().getCode()).body(response);
                    }
                    return ResponseEntity.ok(response);
                })
                .doOnNext(resp -> log.info("chatGatWay response prepared: {}", 
                        resp.getStatusCode()));
    }
} 