package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.entity.UsageInfo;
import com.bgpay.bgai.response.ChatResponse;
import com.bgpay.bgai.service.ApiConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/**
 * 熔断回退服务，处理各种熔断场景
 */
@Service
public class FallbackService {
    
    @Autowired
    private ApiConfigService apiConfigService;
    
    /**
     * 缓存chatCompletionId的Map，用于确保熔断前后ID一致性
     */
    private final Map<String, String> chatCompletionIdCache = new ConcurrentHashMap<>();
    
    /**
     * 当问题为空时的回退处理
     * @return 提示用户输入问题的响应
     */
    public Mono<ResponseEntity<ChatResponse>> handleEmptyQuestionFallback() {
        ChatResponse response = new ChatResponse();
        response.setContent("请提供有效的问题内容，问题不能为空。");
        response.setUsage(new UsageInfo());
        return Mono.just(ResponseEntity.badRequest().body(response));
    }
    
    /**
     * 保存chatCompletionId，用于在熔断情况下保持一致性
     * 
     * @param userId 用户ID，作为缓存键
     * @param chatCompletionId 需要缓存的chatCompletionId
     */
    public void saveChatCompletionId(String userId, String chatCompletionId) {
        if (userId != null && chatCompletionId != null) {
            chatCompletionIdCache.put(userId, chatCompletionId);
        }
    }
    
    /**
     * 获取缓存的chatCompletionId
     * 
     * @param userId 用户ID
     * @return 缓存的chatCompletionId，如果不存在则返回null
     */
    public String getCachedChatCompletionId(String userId) {
        return chatCompletionIdCache.get(userId);
    }
    
    /**
     * 处理熔断情况下的API调用回退，确保chatCompletionId一致性
     * 
     * @param userId 用户ID
     * @param originalError 原始错误
     * @param specificChatCompletionId 指定使用的chatCompletionId
     * @return 带有一致性chatCompletionId的响应
     */
    public Mono<ResponseEntity<ChatResponse>> handleCircuitBreakerFallback(
            String userId, Throwable originalError, String specificChatCompletionId) {
        ChatResponse response = new ChatResponse();
        
        // 设置回退内容
        response.setContent("服务暂时不可用，请稍后重试。错误信息: " + originalError.getMessage());
        
        // 创建并设置UsageInfo，使用指定的chatCompletionId
        UsageInfo usageInfo = new UsageInfo();
        if (specificChatCompletionId != null) {
            usageInfo.setChatCompletionId(specificChatCompletionId);
            // 确保fallback服务也缓存这个ID，以备后续使用
            saveChatCompletionId(userId, specificChatCompletionId);
        } else {
            // 如果没有提供特定ID，使用之前缓存的ID
            String cachedCompletionId = getCachedChatCompletionId(userId);
            if (cachedCompletionId != null) {
                usageInfo.setChatCompletionId(cachedCompletionId);
            } else {
                // 都没有则生成新ID
                usageInfo.setChatCompletionId("fallback-" + UUID.randomUUID().toString());
            }
        }
        
        response.setUsage(usageInfo);
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    /**
     * 通用错误回退处理
     * 
     * @param error 错误信息
     * @return 错误响应
     */
    public Mono<ResponseEntity<ChatResponse>> handleGeneralFallback(Throwable error) {
        ChatResponse response = new ChatResponse();
        response.setContent("处理请求时发生错误: " + error.getMessage());
        response.setUsage(new UsageInfo());
        return Mono.just(ResponseEntity.status(500).body(response));
    }
} 