package com.bgpay.bgai.service.mq;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bgpay.bgai.entity.PriceConfig;
import com.bgpay.bgai.entity.PriceQuery;
import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.bgpay.bgai.entity.UsageRecord;
import com.bgpay.bgai.exception.BillingException;
import com.bgpay.bgai.service.BillingService;
import com.bgpay.bgai.service.PriceCacheService;
import com.bgpay.bgai.service.UsageInfoService;
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

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.Base64;
import java.util.Optional;

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
    @Value("${rocketmq.consumer.group:billing-consumer-group}")
    private String consumerGroup;

    @Value("${rocketmq.name-server:}")
    private String nameServer;
    private final RedisTemplate<String, String> redisTemplate;
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
        String messageId = null;
        try {
            userId = Optional.ofNullable(messageExt.getUserProperty("USER_ID"))
                    .orElseThrow(() -> new BillingException("缺失USER_ID"));
            messageId = messageExt.getMsgId();
            
            // 解析消息体 - 处理Base64编码
            String base64Body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
            if (base64Body.startsWith("\"") && base64Body.endsWith("\"")) {
                base64Body = base64Body.substring(1, base64Body.length() - 1);
            }
            
            // Base64解码
            byte[] jsonBytes = Base64.getDecoder().decode(base64Body);
            String jsonStr = new String(jsonBytes, StandardCharsets.UTF_8);
            
            log.debug("解码后的JSON数据: {}", jsonStr);
            
            // 解析JSON
            UsageCalculationDTO dto = JSON.parseObject(jsonStr, UsageCalculationDTO.class);
            completionId = dto.getChatCompletionId();
            // 补全 userId
            dto.setUserId(userId);
            // 补全 messageId
            dto.setMessageId(messageId);
            // 补全 updatedAt
            if (dto.getUpdatedAt() == null) {
                dto.setUpdatedAt(java.time.LocalDateTime.now());
            }
            
            String redisKey = PROCESSED_KEY_PREFIX + completionId + ":" + messageId;
            String businessKey = userId + ":" + completionId;
            
            // 幂等判断：只要该消息已处理过且为COMPLETED就跳过
            if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                UsageRecord record = usageRecordService.findByCompletionIdAndMessageId(completionId, messageId);
                if (record != null && "COMPLETED".equals(record.getStatus())) {
                    log.info("消息已完全处理，跳过处理, businessKey: {}, messageId: {}", businessKey, messageId);
                    return;
                }
            }
            
            // 保存计费数据到缓存，供后续步骤使用
            usageRecordService.cacheCalculationDTO(completionId, dto);
            
            // 执行第一步：准备处理
            boolean firstStepResult = bgaiService.executeFirstStep(businessKey);
            if (!firstStepResult) {
                log.error("第一步执行失败，开始补偿, businessKey: {}", businessKey);
                bgaiService.compensateFirstStep(businessKey);
                throw new BillingException("第一步执行失败");
            }
            
            // 执行第二步：处理账单消息和数据插入
            boolean secondStepResult = bgaiService.executeSecondStep(businessKey, firstStepResult);
            if (!secondStepResult) {
                log.error("第二步执行失败，开始补偿, businessKey: {}", businessKey);
                bgaiService.compensateSecondStep(businessKey);
                bgaiService.compensateFirstStep(businessKey);
                throw new BillingException("第二步执行失败");
            }
            
            // 执行第三步：更新账单状态
            boolean thirdStepResult = bgaiService.executeThirdStep(businessKey, secondStepResult, messageId);
            if (!thirdStepResult) {
                log.error("第三步执行失败，开始补偿, businessKey: {}", businessKey);
                bgaiService.compensateThirdStep(businessKey, messageId);
                bgaiService.compensateSecondStep(businessKey);
                bgaiService.compensateFirstStep(businessKey);
                throw new BillingException("第三步执行失败");
            }
            
            // 标记消息完全处理完成
            redisTemplate.opsForValue().set(redisKey, "1", 24, TimeUnit.HOURS);
            
        } catch (Exception e) {
            log.error("消息处理失败 [CompletionId={}, UserId={}], error: {}", completionId, userId, e.getMessage(), e);
            if (completionId != null && userId != null) {
                String businessKey = userId + ":" + completionId;
                // 发生异常时执行完整的补偿链
                bgaiService.compensateThirdStep(businessKey, messageId);
                bgaiService.compensateSecondStep(businessKey);
                bgaiService.compensateFirstStep(businessKey);
            }
            throw new BillingException("消息处理失败: " + e.getMessage(), e);
        }
    }


    private boolean isStartup() {
        return System.currentTimeMillis() - startupTime < 60000; // 启动后1分钟内认为是启动阶段
    }
}


