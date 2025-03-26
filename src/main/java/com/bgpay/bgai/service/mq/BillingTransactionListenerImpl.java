package com.bgpay.bgai.service.mq;

import com.alibaba.fastjson2.JSON;
import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.bgpay.bgai.service.UsageInfoService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

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
    private final RedisTemplate<String, String> redisTemplate;
    private final UsageInfoService usageInfoService;

    public BillingTransactionListenerImpl(RedisTemplate<String, String> redisTemplate, UsageInfoService usageInfoService) {
        this.redisTemplate = redisTemplate;
        this.usageInfoService = usageInfoService;
    }

    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        String completionId = this.extractCompletionId(msg, arg);
        if (completionId == null) {
            return RocketMQLocalTransactionState.ROLLBACK;
        }
        return checkProcessed(completionId) ?
                RocketMQLocalTransactionState.COMMIT :
                RocketMQLocalTransactionState.UNKNOWN;
    }


    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        String completionId = extractCompletionId(msg, null);
        return checkProcessed(completionId) ?
                RocketMQLocalTransactionState.COMMIT :
                RocketMQLocalTransactionState.ROLLBACK;
    }


    private String extractCompletionId(Message msg, Object arg) {
        // 优先从arg参数获取
        if (arg instanceof String) {
            return (String) arg;
        }

        // 次从消息头获取
        String keys = msg.getHeaders().get(RocketMQHeaders.KEYS, String.class);
        if (keys != null) {
            return keys;
        }

        // 最后尝试解析消息体
        try {
            UsageCalculationDTO dto = JSON.parseObject((byte[]) msg.getPayload(), UsageCalculationDTO.class);
            return dto.getChatCompletionId();
        } catch (Exception e) {
            log.error("消息体解析失败", e);
            return null;
        }
    }


    public boolean checkProcessed(String completionId) {
        if (localCache.getIfPresent(completionId) != null) {
            return true;
        }
        String redisKey = PROCESSED_KEY_PREFIX + completionId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            localCache.put(completionId, true);
            return true;
        }
        try {
            boolean dbExists = usageInfoService.existsByCompletionId(completionId);
            if (dbExists) {
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