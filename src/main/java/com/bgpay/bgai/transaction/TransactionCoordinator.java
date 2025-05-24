package com.bgpay.bgai.transaction;

import com.bgpay.bgai.entity.ChatCompletions;
import com.bgpay.bgai.service.ChatCompletionsService;
import com.bgpay.bgai.service.impl.FallbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 事务协调器，实现2PC(两阶段提交)和Saga补偿模式
 * 用于确保DeepSeek API调用时chatCompletionId的一致性
 */
@Service
@Slf4j
public class TransactionCoordinator {

    private static final String TRANSACTION_KEY_PREFIX = "TX:CHAT:";
    private static final String COMPLETION_ID_PREFIX = "chat-";
    private static final long TRANSACTION_TIMEOUT = 30;
    private static final long COMPENSATION_TIMEOUT = 24;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private FallbackService fallbackService;
    
    @Autowired
    private ChatCompletionsService chatCompletionsService;
    
    // 本地内存缓存，用于快速访问进行中事务的状态
    private final Map<String, TransactionStatus> localTransactionCache = new ConcurrentHashMap<>();
    
    /**
     * 第一阶段：准备事务，生成一个唯一的chatCompletionId
     * 
     * @param userId 用户ID
     * @return 生成的chatCompletionId
     */
    public String prepare(String userId) {
        // 检查是否已存在进行中的事务
        String existingId = getCurrentCompletionId(userId);
        if (existingId != null) {
            log.warn("Found existing transaction for user {}: {}", userId, existingId);
            return existingId;
        }
        
        // 生成唯一的chatCompletionId
        String chatCompletionId = COMPLETION_ID_PREFIX + UUID.randomUUID().toString();
        
        // 使用Redis的事务特性确保原子性
        String transactionKey = TRANSACTION_KEY_PREFIX + userId;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(transactionKey, chatCompletionId, TRANSACTION_TIMEOUT, TimeUnit.MINUTES);
        
        if (Boolean.TRUE.equals(success)) {
            // 存入本地缓存和服务缓存
            localTransactionCache.put(userId, new TransactionStatus(chatCompletionId, TransactionPhase.PREPARED));
            fallbackService.saveChatCompletionId(userId, chatCompletionId);
            
            log.info("Transaction prepared for user {}: chatCompletionId={}", userId, chatCompletionId);
            return chatCompletionId;
        } else {
            // 如果设置失败，说明已经存在事务，返回现有事务ID
            String currentId = redisTemplate.opsForValue().get(transactionKey);
            log.warn("Failed to prepare new transaction, using existing one: {}", currentId);
            return currentId;
        }
    }
    
    /**
     * 第二阶段：提交事务，持久化chatCompletionId
     * 
     * @param userId 用户ID
     * @param chatCompletionId 要提交的chatCompletionId
     * @return 是否成功提交
     */
    @Transactional
    public boolean commit(String userId, String chatCompletionId,String modelName) {
        try {
            // 验证一致性
            String storedId = redisTemplate.opsForValue().get(TRANSACTION_KEY_PREFIX + userId);
            if (storedId == null || !storedId.equals(chatCompletionId)) {
                log.error("Transaction consistency violation: expected={}, actual={}", storedId, chatCompletionId);
                return false;
            }
            
            // 更新事务状态
            localTransactionCache.put(userId, new TransactionStatus(chatCompletionId, TransactionPhase.COMMITTED));
            
            // 持久化记录
            ChatCompletions completion = new ChatCompletions();
            completion.setApiKeyId(chatCompletionId);
            completion.setCreated(System.currentTimeMillis());
            completion.setModel(modelName);
            completion.setObject("chat.completion");
            completion.setSystemFingerprint(userId);
            
            chatCompletionsService.insertChatCompletions(completion);
            
            log.info("Transaction committed for user {}: chatCompletionId={}", userId, chatCompletionId);
            return true;
        } catch (Exception e) {
            log.error("Failed to commit transaction for user {}: {}", userId, e.getMessage(), e);
            // 触发补偿
            compensate(userId, chatCompletionId);
            return false;
        }
    }
    
    /**
     * Saga补偿模式：当提交失败时执行补偿操作
     * 
     * @param userId 用户ID
     * @param chatCompletionId 需要补偿的chatCompletionId
     */
    public void compensate(String userId, String chatCompletionId) {
        try {
            log.info("Compensating transaction for user {}: chatCompletionId={}", userId, chatCompletionId);
            
            // 更新事务状态
            localTransactionCache.put(userId, new TransactionStatus(chatCompletionId, TransactionPhase.COMPENSATED));
            
            // 记录失败但保持ID一致性
            String transactionKey = TRANSACTION_KEY_PREFIX + userId + ":COMPENSATED";
            redisTemplate.opsForValue().set(transactionKey, chatCompletionId, COMPENSATION_TIMEOUT, TimeUnit.HOURS);
            
            // 确保fallback服务仍然有这个ID的记录
            fallbackService.saveChatCompletionId(userId, chatCompletionId);
        } catch (Exception e) {
            log.error("Failed to compensate transaction for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 回滚事务
     * 
     * @param userId 用户ID
     * @return 回滚的chatCompletionId
     */
    public String rollback(String userId) {
        try {
            // 获取当前事务状态
            TransactionStatus status = localTransactionCache.get(userId);
            String chatCompletionId = status != null ? status.getChatCompletionId() 
                    : redisTemplate.opsForValue().get(TRANSACTION_KEY_PREFIX + userId);
            
            if (chatCompletionId == null) {
                log.warn("No active transaction found for user {} during rollback", userId);
                // 生成一个回滚专用的ID
                chatCompletionId = COMPLETION_ID_PREFIX + "rollback-" + UUID.randomUUID().toString();
            }
            
            // 更新事务状态
            localTransactionCache.put(userId, new TransactionStatus(chatCompletionId, TransactionPhase.ROLLEDBACK));
            
            // 记录回滚状态
            String rollbackKey = TRANSACTION_KEY_PREFIX + userId + ":ROLLEDBACK";
            redisTemplate.opsForValue().set(rollbackKey, chatCompletionId, COMPENSATION_TIMEOUT, TimeUnit.HOURS);
            
            log.info("Transaction rolled back for user {}: chatCompletionId={}", userId, chatCompletionId);
            return chatCompletionId;
        } catch (Exception e) {
            log.error("Failed to rollback transaction for user {}: {}", userId, e.getMessage(), e);
            // 生成一个紧急回滚ID
            String emergencyId = COMPLETION_ID_PREFIX + "emergency-" + UUID.randomUUID().toString();
            fallbackService.saveChatCompletionId(userId, emergencyId);
            return emergencyId;
        }
    }
    
    /**
     * 获取当前事务的chatCompletionId
     * 
     * @param userId 用户ID
     * @return 当前的chatCompletionId，如果不存在则返回null
     */
    public String getCurrentCompletionId(String userId) {
        String transactionKey = TRANSACTION_KEY_PREFIX + userId;
        
        // 首先查询Redis，因为Redis是分布式一致的数据源
        String redisId = redisTemplate.opsForValue().get(transactionKey);
        if (redisId != null) {
            // 更新本地缓存
            localTransactionCache.put(userId, new TransactionStatus(redisId, TransactionPhase.PREPARED));
            return redisId;
        }
        
        // 如果Redis中没有，检查本地缓存
        TransactionStatus status = localTransactionCache.get(userId);
        if (status != null) {
            // 验证本地缓存的状态
            if (status.getPhase() == TransactionPhase.PREPARED) {
                // 尝试恢复Redis中的数据
                redisTemplate.opsForValue().setIfAbsent(transactionKey, status.getChatCompletionId(), TRANSACTION_TIMEOUT, TimeUnit.MINUTES);
                return status.getChatCompletionId();
            } else {
                // 如果不是PREPARED状态，清除本地缓存
                localTransactionCache.remove(userId);
            }
        }
        
        return null;
    }
    
    /**
     * 检查是否存在进行中的事务
     * 
     * @param userId 用户ID
     * @return 是否存在进行中的事务
     */
    public boolean hasActiveTransaction(String userId) {
        return getCurrentCompletionId(userId) != null;
    }
} 