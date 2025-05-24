package com.bgpay.bgai.service.mq;

import com.alibaba.fastjson2.JSON;
import com.bgpay.bgai.entity.PriceConfig;
import com.bgpay.bgai.entity.PriceQuery;
import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.bgpay.bgai.entity.UsageRecord;
import com.bgpay.bgai.exception.BillingException;
import com.bgpay.bgai.service.BillingService;
import com.bgpay.bgai.service.PriceCacheService;
import com.bgpay.bgai.service.UsageRecordService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.bgpay.bgai.entity.PriceConstants.*;
import static com.bgpay.bgai.entity.PriceConstants.INPUT_TYPE;

@Service
@Component
@RequiredArgsConstructor
@Slf4j
public class RocketMQBillingServiceImpl implements BillingService {
    private static final String BILLING_TOPIC = "BILLING_TOPIC";
    private static final String BILLING_TAG = "USER_BILLING";
    private static final String LOCK_KEY_PREFIX = "BILLING_LOCK:";
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalTime DISCOUNT_START = LocalTime.of(0, 30);
    private static final LocalTime DISCOUNT_END = LocalTime.of(8, 30);
    @Value("${rocketmq.consumer.group:billing-consumer-group}")
    private String consumerGroup;

    @Value("${rocketmq.name-server:}")
    private String nameServer;
    private final RedisTemplate<String, String> redisTemplate;
    private final PriceCacheService priceCache;
    private final UsageRecordService usageRecordService;
    private final MeterRegistry meterRegistry;
    private final RocketMQProducerService mqProducer;

    private final MQConsumerService mqConsumerService;


    private final Cache<String, Boolean> localCache = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    @Override
    @Async("billingExecutor")
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void processBatch(List<UsageCalculationDTO> batch, String userId) {
        batch.parallelStream()
                .forEach(dto -> processSingleRecord(dto, userId));
    }

    @Override
    public void processSingleRecord(UsageCalculationDTO dto, String userId) {
        mqProducer.sendBillingMessage(dto, userId);
    }
    @PostConstruct
    public void initConsumer() throws MQClientException {
        mqConsumerService.initConsumer(
                nameServer,
                consumerGroup,
                BILLING_TOPIC,
                BILLING_TAG,
                this::processMessage,  // 方法引用处理逻辑
                msg -> log.info("Message consumed: {}", msg.getMsgId())
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void processMessage(MessageExt messageExt) {
        String completionId = null;
        try {
            String userId = Optional.ofNullable(messageExt.getUserProperty("USER_ID"))
                    .orElseThrow(() -> new BillingException("缺失USER_ID"));
            UsageCalculationDTO dto = deserializeMessageBody(messageExt);
            completionId = dto.getChatCompletionId();

            // 前置幂等检查
            if (checkProcessed(completionId)) {
                log.debug("消息已处理 [CompletionId={}]", completionId);
                return;
            }

            String finalCompletionId = completionId;
            processWithDistributedLock(userId, completionId, () -> {
                // 锁内二次幂等检查
                if (checkProcessed(finalCompletionId)) {
                    log.debug("消息已处理 [CompletionId={}]", finalCompletionId);
                    return null;
                }

                ZonedDateTime beijingTime = convertToBeijingTime(dto.getCreatedAt());
                String timePeriod = determineTimePeriod(beijingTime);

                BigDecimal inputCost = calculateInputCost(dto, timePeriod);
                BigDecimal outputCost = calculateOutputCost(dto, timePeriod);

                UsageRecord record = convertToEntity(dto, inputCost, outputCost, userId, timePeriod);
                usageRecordService.insertUsageRecord(record);

                // 异步更新缓存，确保主流程快速完成
                updateProcessedCache(finalCompletionId);
                return null;
            });
        } catch (DuplicateKeyException e) {
            log.warn("重复记录 [CompletionId={}]", completionId);
            updateProcessedCache(completionId);
        } catch (Exception e) {
            throw new BillingException("消息处理失败", e);
        }
    }

    private void updateProcessedCache(String completionId) {
        localCache.put(completionId, true);
        // 异步更新Redis
        CompletableFuture.runAsync(() -> {
            String redisKey = "PROCESSED:" + completionId;
            redisTemplate.opsForValue().set(redisKey, "1", 24, TimeUnit.HOURS);
        }).exceptionally(e -> {
            log.error("更新Redis缓存失败: {}", e.getMessage());
            return null;
        });
    }

    private <T> T processWithDistributedLock(String userId, String completionId, Callable<T> callback) {
        String lockKey = LOCK_KEY_PREFIX + userId + ":" + completionId;
        try {
            // 设置较短的锁超时时间（如5秒）
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(
                    lockKey, "processing", 5, TimeUnit.SECONDS
            );
            if (!Boolean.TRUE.equals(locked)) {
                throw new BillingException("系统繁忙，请稍后重试");
            }
            return callback.call();
        } catch (Exception e) {
            throw new BillingException("处理异常", e);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }


    private UsageCalculationDTO deserializeMessageBody(MessageExt messageExt) {
        return JSON.parseObject(messageExt.getBody(), UsageCalculationDTO.class);
    }


    private boolean checkProcessed(String completionId) {
        // 本地缓存检查
        if (localCache.getIfPresent(completionId) != null) return false;

        // Redis检查
        String redisKey = "PROCESSED:" + completionId;
        Boolean exists = redisTemplate.hasKey(redisKey);
        if (exists != null && exists) {
            localCache.put(completionId, true);
            return false;
        }

        // 数据库检查（兜底）
        boolean dbExists = usageRecordService.existsByCompletionId(completionId);
        if (dbExists) {
            // 异步设置Redis，避免阻塞
            CompletableFuture.runAsync(() ->
                    redisTemplate.opsForValue().set(redisKey, "1", 24, TimeUnit.HOURS)
            );
            localCache.put(completionId, true);
            return true;
        }
        return false;
    }

    private UsageRecord convertToEntity(UsageCalculationDTO dto, BigDecimal inputCost, BigDecimal outputCost, String userId, String timePeriod) {
        UsageRecord record = new UsageRecord();
        record.setModelType(dto.getModelType());
        record.setChatCompletionId(dto.getChatCompletionId());
        record.setUserId(userId);
        record.setInputCost(inputCost);
        record.setOutputCost(outputCost);
        record.setCalculatedAt(LocalDateTime.now());
        record.setStatus("PENDING");
        record.setCreatedAt(LocalDateTime.now());
        
        // 获取价格版本
        try {
            Integer priceVersion = getPriceVersion(dto, timePeriod);
            record.setPriceVersion(priceVersion);
        } catch (BillingException e) {
            log.warn("Failed to get price version, using default version 1: {}", e.getMessage());
            record.setPriceVersion(1);
        }
        
        return record;
    }

    private Integer getPriceVersion(UsageCalculationDTO dto, String timePeriod) {
        // Create a price query object for output
        PriceQuery query = new PriceQuery(
                dto.getModelType(),
                timePeriod,
                null,
                OUTPUT_TYPE
        );

        PriceConfig config = priceCache.getPriceConfig(query);

        if (config == null) {
            throw new BillingException("Price config not found");
        } else if (!(config instanceof PriceConfig)) {
            log.error("refresh Cache config by ModelType: {}", dto.getModelType());
            priceCache.refreshCacheByModel(dto.getModelType());
        }
        Integer cachedVersion = config.getVersion();
        log.info("Output price config priceVersion: {}", cachedVersion);
        return cachedVersion;
    }

    private ZonedDateTime convertToBeijingTime(LocalDateTime utcTime) {
        return utcTime.atZone(ZoneOffset.UTC)
                .withZoneSameInstant(BEIJING_ZONE);
    }

    private String determineTimePeriod(ZonedDateTime beijingTime) {
        LocalDate date = beijingTime.toLocalDate();

        ZonedDateTime discountStart = ZonedDateTime.of(date, DISCOUNT_START, BEIJING_ZONE);
        ZonedDateTime discountEnd = ZonedDateTime.of(date, DISCOUNT_END, BEIJING_ZONE);

        if (discountEnd.isBefore(discountStart)) {
            discountEnd = discountEnd.plusDays(1);
        }

        return (beijingTime.isAfter(discountStart) && beijingTime.isBefore(discountEnd))
                ? "discount" : "standard";
    }

    private BigDecimal calculateInputCost(UsageCalculationDTO dto, String timePeriod) {
        String cacheStatus = dto.getPromptCacheHitTokens() > 0 ? CACHE_HIT : CACHE_MISS;
        PriceQuery query = new PriceQuery(dto.getModelType(), timePeriod,
                cacheStatus, INPUT_TYPE);
        PriceConfig config = priceCache.getPriceConfig(query);
        int totalTokens = dto.getPromptCacheHitTokens() +
                dto.getPromptCacheMissTokens();
        return calculateTokenCost(totalTokens, config.getPrice());
    }

    private BigDecimal calculateTokenCost(int tokens, BigDecimal price) {
        return BigDecimal.valueOf(tokens)
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP)
                .multiply(price)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateOutputCost(UsageCalculationDTO usage, String timePeriod) {
        PriceQuery query = new PriceQuery(
                usage.getModelType(),
                timePeriod,
                null,
                OUTPUT_TYPE
        );

        PriceConfig config = priceCache.getPriceConfig(query);
        if (config == null) {
            throw new BillingException("Price config not found");
        } else if (!(config instanceof PriceConfig)) {
            log.error("Invalid cache data type: {}", config.getClass());
            priceCache.refreshCacheByModel(usage.getModelType());
            return calculateOutputCost(usage, timePeriod);
        }
        return calculateCost(usage.getCompletionTokens(), config.getPrice());
    }

    private BigDecimal calculateCost(int tokens, BigDecimal pricePerMillion) {
        return BigDecimal.valueOf(tokens)
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP)
                .multiply(pricePerMillion)
                .setScale(4, RoundingMode.HALF_UP);
    }
}


