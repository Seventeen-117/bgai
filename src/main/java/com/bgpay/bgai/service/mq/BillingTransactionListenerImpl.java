package com.bgpay.bgai.service.mq;

import com.alibaba.fastjson2.JSON;
import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.bgpay.bgai.entity.UsageInfo;
import com.bgpay.bgai.service.UsageInfoService;
import com.bgpay.bgai.transaction.TransactionCoordinator;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Component
@RocketMQTransactionListener()
@Slf4j
public class BillingTransactionListenerImpl implements RocketMQLocalTransactionListener {
    private static final String PROCESSED_KEY_PREFIX = "PROCESSED:";
    private static final int LOCAL_CACHE_MAX_SIZE = 100_000;
    private static final int LOCAL_CACHE_EXPIRE_MINUTES = 5;
    private static final int REDIS_CACHE_EXPIRE_HOURS = 24;

    private final Cache<String, Boolean> localCache = Caffeine.newBuilder()
            .maximumSize(LOCAL_CACHE_MAX_SIZE)
            .expireAfterWrite(LOCAL_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
            .build();
            
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private UsageInfoService usageInfoService;
    
    @Autowired
    private TransactionCoordinator transactionCoordinator;

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        String xid = RootContext.getXID();
        String userId = msg.getHeaders().get("USER_ID", String.class);
        String completionId = extractCompletionId(msg, arg);
        
        try {
            log.info("开始执行本地事务, xid: {}, userId: {}, completionId: {}", xid, userId, completionId);
            
            // 检查是否已处理
            if (checkProcessed(completionId)) {
                log.info("消息已处理，提交事务, completionId: {}", completionId);
                return RocketMQLocalTransactionState.COMMIT;
            }
            
            // 验证事务ID是否存在且一致
            String currentId = transactionCoordinator.getCurrentCompletionId(userId);
            if (currentId == null || !currentId.equals(completionId)) {
                log.error("事务ID不一致或不存在，回滚事务, expected: {}, actual: {}", completionId, currentId);
                return RocketMQLocalTransactionState.ROLLBACK;
            }
            
            // 执行本地事务逻辑
            UsageCalculationDTO dto = JSON.parseObject((byte[]) msg.getPayload(), UsageCalculationDTO.class);
            boolean success = usageInfoService.processUsageInfo(dto, userId);
            
            if (success) {
                // 提交事务
                if (transactionCoordinator.commit(userId, completionId, dto.getModelType())) {
                    log.info("本地事务执行成功，提交事务, completionId: {}", completionId);
                    return RocketMQLocalTransactionState.COMMIT;
                }
            }
            
            // 执行失败，回滚事务
            log.warn("本地事务执行失败，回滚事务, completionId: {}", completionId);
            transactionCoordinator.compensate(userId, completionId);
            return RocketMQLocalTransactionState.ROLLBACK;
            
        } catch (Exception e) {
            log.error("本地事务执行异常，回滚事务, xid: {}, completionId: {}", xid, completionId, e);
            transactionCoordinator.compensate(userId, completionId);
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        String completionId = extractCompletionId(msg, null);
        String userId = msg.getHeaders().get("USER_ID", String.class);
        
        try {
            log.info("检查本地事务状态, userId: {}, completionId: {}", userId, completionId);
            
            // 检查事务状态
            if (transactionCoordinator.hasActiveTransaction(userId)) {
                String currentId = transactionCoordinator.getCurrentCompletionId(userId);
                if (completionId.equals(currentId)) {
                    log.info("事务活跃且ID一致，提交事务, completionId: {}", completionId);
                    return RocketMQLocalTransactionState.COMMIT;
                }
            }
            
            // 检查是否已处理
            if (checkProcessed(completionId)) {
                log.info("消息已处理，提交事务, completionId: {}", completionId);
                return RocketMQLocalTransactionState.COMMIT;
            }
            
            // 回滚事务
            log.warn("未找到活跃事务或消息未处理，回滚事务, completionId: {}", completionId);
            return RocketMQLocalTransactionState.ROLLBACK;
            
        } catch (Exception e) {
            log.error("检查本地事务状态异常, completionId: {}", completionId, e);
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    private String extractCompletionId(Message msg, Object arg) {
        if (arg instanceof String) {
            return (String) arg;
        }

        String keys = msg.getHeaders().get(RocketMQHeaders.KEYS, String.class);
        if (keys != null) {
            return keys;
        }

        try {
            UsageCalculationDTO dto = JSON.parseObject((byte[]) msg.getPayload(), UsageCalculationDTO.class);
            return dto.getChatCompletionId();
        } catch (Exception e) {
            log.error("消息体解析失败", e);
            return null;
        }
    }

    private boolean checkProcessed(String completionId) {
        if (completionId == null) {
            return false;
        }
        
        // 检查本地缓存
        if (localCache.getIfPresent(completionId) != null) {
            return true;
        }
        
        // 检查Redis缓存
        String redisKey = PROCESSED_KEY_PREFIX + completionId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            localCache.put(completionId, true);
            return true;
        }
        
        // 检查数据库
        try {
            boolean dbExists = usageInfoService.existsByCompletionId(completionId);
            if (dbExists) {
                // 异步更新缓存
                redisTemplate.opsForValue().set(redisKey, "1", REDIS_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                localCache.put(completionId, true);
            }
            return dbExists;
        } catch (Exception e) {
            log.error("数据库检查失败，completionId: {}", completionId, e);
            return false;
        }
    }
}