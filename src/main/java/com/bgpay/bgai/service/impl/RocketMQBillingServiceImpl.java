package com.bgpay.bgai.service.impl;

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
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
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
    private final RocketMQTemplate rocketMQTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final PriceCacheService priceCache;
    private final UsageRecordService usageRecordService;
    private final MeterRegistry meterRegistry;

    // 本地缓存防止重复检查
    private final Cache<String, Boolean> localCache = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    // 新增消费成功回调接口
    public interface ConsumeCallback {
        void onSuccess(MessageExt message);
    }

    private ConsumeCallback consumeCallback = message ->
            log.info("Message consumed successfully: {}", message.getMsgId());

    @Override
    @Async("billingExecutor")
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void processBatch(List<UsageCalculationDTO> batch, String userId) {
        batch.parallelStream()
                .forEach(dto -> processSingleRecord(dto, userId));
    }

    @Override
    public void processSingleRecord(UsageCalculationDTO dto, String userId) {
        Message<UsageCalculationDTO> message = MessageBuilder.withPayload(dto)
                .setHeader(RocketMQHeaders.KEYS, dto.getChatCompletionId())
                .setHeader(RocketMQHeaders.TAGS, BILLING_TAG)
                .setHeader("USER_ID", userId)
                .build();
        TransactionSendResult result = rocketMQTemplate.sendMessageInTransaction(
                BILLING_TOPIC,
                message,
                dto.getChatCompletionId()
        );

        if (result.getSendStatus() == SendStatus.SEND_OK) {
            meterRegistry.counter("billing.message.sent").increment();
            log.debug("消息发送状态: {}", result.getSendStatus());
        } else {
            log.error("消息发送失败，状态: {}", result.getSendStatus());
        }
    }

    @PostConstruct
    public void initConsumer() throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServer);
        consumer.setConsumeThreadMin(30);
        consumer.setConsumeThreadMax(50);
        consumer.setConsumeMessageBatchMaxSize(50);
        consumer.setMaxReconsumeTimes(5);
        consumer.subscribe(BILLING_TOPIC, BILLING_TAG);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);

        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            Map<MessageQueue, Long> offsetMap = new HashMap<>();
            List<MessageExt> successMessages = new ArrayList<>();
            boolean hasFailure = false;

            for (MessageExt msg : msgs) {
                try {
                    processMessage(msg);
                    successMessages.add(msg);
                    // 记录最大offset
                    MessageQueue mq = new MessageQueue(msg.getTopic(),
                            msg.getBrokerName(), msg.getQueueId());
                    offsetMap.put(mq, Math.max(offsetMap.getOrDefault(mq, -1L), msg.getQueueOffset()));
                } catch (Exception e) {
                    log.error("Message consumption failed [MsgId={}]", msg.getMsgId(), e);
                    hasFailure = true;
                }
            }

            if (hasFailure) {
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }

            // 手动提交offset
            offsetMap.forEach((mq, offset) -> {
                consumer.getOffsetStore().updateOffset(mq, offset + 1, false);
                consumer.getOffsetStore().persist(mq);
            });

            // 执行回调
            successMessages.forEach(msg -> {
                consumeCallback.onSuccess(msg);
                meterRegistry.counter("billing.message.consumed").increment();
            });

            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        consumer.start();
        log.info("Billing consumer started successfully");
    }

    @Transactional(rollbackFor = Exception.class)
    public void processMessage(MessageExt messageExt) {
        try {
            String userId = Optional.ofNullable(messageExt.getUserProperty("USER_ID"))
                    .orElseThrow(() -> new BillingException("缺失USER_ID"));

            // 修改为调用 deserializeMessageBody 方法
            UsageCalculationDTO dto = deserializeMessageBody(messageExt);
            String completionId = dto.getChatCompletionId();

            if (checkProcessed(completionId)) {
                log.debug("消息已处理 [CompletionId={}]", completionId);
                return;
            }

            processWithDistributedLock(userId, completionId, () -> {
                ZonedDateTime beijingTime = convertToBeijingTime(dto.getCreatedAt());
                String timePeriod = determineTimePeriod(beijingTime);

                BigDecimal inputCost = calculateInputCost(dto, timePeriod);
                BigDecimal outputCost = calculateOutputCost(dto, timePeriod);

                UsageRecord record = convertToEntity(dto, inputCost, outputCost, userId, timePeriod);
                usageRecordService.insertUsageRecord(record);

                updateProcessedCache(completionId);
                return null;
            });
        } catch (DuplicateKeyException e) {
            log.warn("重复记录 [CompletionId={}]", messageExt.getKeys());
            updateProcessedCache(messageExt.getKeys());
        } catch (Exception e) {
            throw new BillingException("消息处理失败", e);
        }
    }

    private void updateProcessedCache(String completionId) {
        String redisKey = "PROCESSED:" + completionId;
        redisTemplate.opsForValue().set(redisKey, "1", 24, TimeUnit.HOURS);
        localCache.put(completionId, true);
    }

    private <T> T processWithDistributedLock(String userId, String completionId, Callable<T> callback) {
        String lockKey = LOCK_KEY_PREFIX + userId + ":" + completionId;
        try {
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(
                    lockKey, "processing", 30, TimeUnit.SECONDS
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
        // 第一层：本地缓存检查
        if (localCache.getIfPresent(completionId) != null) return true;

        // 第二层：Redis检查
        String redisKey = "PROCESSED:" + completionId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            localCache.put(completionId, true);
            return false;
        }

        // 第三层：数据库检查
        boolean exists = usageRecordService.existsByCompletionId(completionId);
        if (exists) {
            redisTemplate.opsForValue().set(redisKey, "1", 24, TimeUnit.HOURS);
        }
        return exists;
    }

    private UsageRecord convertToEntity(UsageCalculationDTO dto, BigDecimal inputCost, BigDecimal outputCost, String userId, String timePeriod) {
        UsageRecord record = new UsageRecord();
        record.setModelType(dto.getModelType());
        record.setChatCompletionId(dto.getChatCompletionId());
        record.setUserId(userId);
        record.setInputCost(inputCost);
        record.setOutputCost(outputCost);
        Integer priceVersion = getPriceVersion(dto, timePeriod);
        record.setPriceVersion(priceVersion);
        record.setCalculatedAt(LocalDateTime.now());
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
        // Determine the cache status based on the number of cached tokens
        String cacheStatus = dto.getPromptCacheHitTokens() > 0 ? CACHE_HIT : CACHE_MISS;
        // Create a price query object for input
        PriceQuery query = new PriceQuery(dto.getModelType(), timePeriod,
                cacheStatus, INPUT_TYPE);

        // Get the price configuration from the cache
        PriceConfig config = priceCache.getPriceConfig(query);

        // Calculate the total number of tokens
        int totalTokens = dto.getPromptCacheHitTokens() +
                dto.getPromptCacheMissTokens();
        // Calculate the cost based on the total number of tokens and the price
        return calculateTokenCost(totalTokens, config.getPrice());
    }

    private BigDecimal calculateTokenCost(int tokens, BigDecimal price) {
        return BigDecimal.valueOf(tokens)
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP)
                .multiply(price)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateOutputCost(UsageCalculationDTO usage, String timePeriod) {
        // Create a price query object for output
        PriceQuery query = new PriceQuery(
                usage.getModelType(),
                timePeriod,
                null,
                OUTPUT_TYPE
        );

        PriceConfig config = priceCache.getPriceConfig(query);
        if (config == null) {
            throw new BillingException("Price config not found");
        } else if (!(config instanceof PriceConfig)) { // 冗余检查，实际由 RedisTemplate 保证类型
            log.error("Invalid cache data type: {}", config.getClass());
            priceCache.refreshCacheByModel(usage.getModelType()); // 触发缓存刷新
            return calculateOutputCost(usage, timePeriod); // 重试
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