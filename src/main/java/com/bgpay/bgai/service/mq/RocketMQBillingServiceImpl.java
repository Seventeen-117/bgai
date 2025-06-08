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
import com.bgpay.bgai.service.impl.BGAIServiceImpl;
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
    private static final String PROCESSED_KEY_PREFIX = "PROCESSED:";
    private static final int REDIS_CACHE_EXPIRE_HOURS = 24;
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
    private final RocketMQProducerService mqProducer;
    private final MQConsumerService mqConsumerService;
    private final BGAIServiceImpl bgaiService;

    private final Cache<String, Boolean> localCache = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    private final long startupTime = System.currentTimeMillis();

    @Override
    @Async("billingExecutor")
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void processBatch(List<UsageCalculationDTO> batch, String userId) {
        batch.parallelStream()
                .forEach(dto -> processSingleRecord(dto, userId));
    }

    @Override
    public void processSingleRecord(UsageCalculationDTO dto, String userId) {
        // 启动时不发送消息，只在API调用时发送
        if (!isStartup()) {
            mqProducer.sendBillingMessage(dto, userId);
        } else {
            log.info("Skipping message sending during startup for userId: {}, completionId: {}", 
                    userId, dto.getChatCompletionId());
        }
    }

    @PostConstruct
    public void initConsumer() throws MQClientException {
        // 只初始化消费者，不发送消息
        mqConsumerService.initConsumer(
                nameServer,
                consumerGroup,
                BILLING_TOPIC,
                BILLING_TAG,
                this::processMessage,
                msg -> log.info("Message consumed: {}", msg.getMsgId())
        );
        log.info("Billing consumer initialized successfully. Ready to process unconsumed messages.");
    }

    @Transactional(rollbackFor = Exception.class)
    public void processMessage(MessageExt messageExt) {
        String completionId = null;
        String userId = null;
        try {
            userId = Optional.ofNullable(messageExt.getUserProperty("USER_ID"))
                    .orElseThrow(() -> new BillingException("缺失USER_ID"));
            UsageCalculationDTO dto = deserializeMessageBody(messageExt);
            completionId = dto.getChatCompletionId();
            
            String businessKey = userId + ":" + completionId;
            
            // 检查是否已处理过
            if (checkProcessed(businessKey)) {
                log.info("Message already processed, skipping. businessKey: {}", businessKey);
                return;
            }
            
            // 在启动阶段，只处理未消费的消息，不发送新消息
            if (isStartup()) {
                log.info("System is in startup phase, processing unconsumed message: {}", businessKey);
                processUnconsumedMessage(dto, userId, businessKey);
                return;
            }
            
            // 执行第一步：发送账单消息
            boolean firstStepResult = bgaiService.executeFirstStep(businessKey);
            if (!firstStepResult) {
                log.error("第一步执行失败，开始补偿, businessKey: {}", businessKey);
                bgaiService.compensateFirstStep(businessKey);
                throw new BillingException("第一步执行失败");
            }
            
            // 执行第二步：处理账单消息
            boolean secondStepResult = bgaiService.executeSecondStep(businessKey, firstStepResult);
            if (!secondStepResult) {
                log.error("第二步执行失败，开始补偿, businessKey: {}", businessKey);
                bgaiService.compensateSecondStep(businessKey);
                bgaiService.compensateFirstStep(businessKey);
                throw new BillingException("第二步执行失败");
            }
            
            // 执行第三步：更新账单状态
            boolean thirdStepResult = bgaiService.executeThirdStep(businessKey, secondStepResult);
            if (!thirdStepResult) {
                log.error("第三步执行失败，开始补偿, businessKey: {}", businessKey);
                bgaiService.compensateThirdStep(businessKey);
                bgaiService.compensateSecondStep(businessKey);
                bgaiService.compensateFirstStep(businessKey);
                throw new BillingException("第三步执行失败");
            }
            
            // 标记消息已处理
            markAsProcessed(businessKey);
            log.info("消息处理成功完成, businessKey: {}", businessKey);
            
        } catch (Exception e) {
            log.error("消息处理失败 [CompletionId={}, UserId={}]", completionId, userId, e);
            if (completionId != null && userId != null) {
                String businessKey = userId + ":" + completionId;
                // 发生异常时执行完整的补偿链
                bgaiService.compensateThirdStep(businessKey);
                bgaiService.compensateSecondStep(businessKey);
                bgaiService.compensateFirstStep(businessKey);
            }
            throw new BillingException("消息处理失败", e);
        }
    }

    private void processUnconsumedMessage(UsageCalculationDTO dto, String userId, String businessKey) {
        try {
            // 直接处理消息，不发送新消息
            UsageRecord record = convertToEntity(dto, dto.getInputCost(), dto.getOutputCost(), userId, determineTimePeriod(convertToBeijingTime(dto.getCreatedAt())));
            usageRecordService.insertUsageRecord(record);
            markAsProcessed(businessKey);
            log.info("Successfully processed unconsumed message during startup: {}", businessKey);
        } catch (Exception e) {
            log.error("Failed to process unconsumed message during startup: {}", businessKey, e);
            throw new BillingException("Failed to process unconsumed message", e);
        }
    }

    private boolean checkProcessed(String businessKey) {
        String redisKey = PROCESSED_KEY_PREFIX + businessKey;
        return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
    }

    private void markAsProcessed(String businessKey) {
        String redisKey = PROCESSED_KEY_PREFIX + businessKey;
        redisTemplate.opsForValue().set(redisKey, "1", REDIS_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    private UsageCalculationDTO deserializeMessageBody(MessageExt messageExt) {
        byte[] body = messageExt.getBody();
        String base64Str = new String(body, java.nio.charset.StandardCharsets.UTF_8).trim();
        if (base64Str.startsWith("\"") && base64Str.endsWith("\"")) {
            base64Str = base64Str.substring(1, base64Str.length() - 1);
        }
        byte[] jsonBytes = java.util.Base64.getDecoder().decode(base64Str);
        String jsonStr = new String(jsonBytes, java.nio.charset.StandardCharsets.UTF_8);
        return com.alibaba.fastjson2.JSON.parseObject(jsonStr, UsageCalculationDTO.class);
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

    private boolean isStartup() {
        return System.currentTimeMillis() - startupTime < 60000; // 启动后1分钟内认为是启动阶段
    }
}


