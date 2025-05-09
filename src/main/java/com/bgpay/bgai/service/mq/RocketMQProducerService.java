package com.bgpay.bgai.service.mq;


import com.alibaba.fastjson2.JSON;
import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.bgpay.bgai.exception.BillingException;
import com.bgpay.bgai.response.ChatResponse;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RocketMQProducerService {
    private static final String BILLING_TOPIC = "BILLING_TOPIC";
    private static final String BILLING_TAG = "USER_BILLING";

    @Value("${rocketmq.name-server}")
    private String namesrvAddr;

    @Value("${rocketmq.producer.group}")
    private String producerGroup;

    @Value("${rocketmq.topic.chat-log}")
    private String chatLogTopic;

    private DefaultMQProducer producer;

    private final RocketMQTemplate rocketMQTemplate;

    private static final String BILLING_DESTINATION = "BILLING_TOPIC:USER_BILLING";

    public RocketMQProducerService(
            @Value("${rocketmq.name-server}") String namesrvAddr,
            @Value("${rocketmq.producer.group}") String producerGroup,
            RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }



    @PostConstruct
    public void init() throws Exception {
        producer = new DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(namesrvAddr);

        // 新增网络优化参数
        producer.setSendMsgTimeout(15000);
        producer.setRetryTimesWhenSendFailed(5);
        producer.setCompressMsgBodyOverHowmuch(1024*4);
        producer.setMaxMessageSize(1024*128);

        // 启用VIP通道（需Broker支持）
        producer.setVipChannelEnabled(true);

        producer.start();
    }

    @PreDestroy
    public void destroy() {
        if (producer != null) {
            producer.shutdown();
        }
    }
    private final Cache<String, Boolean> idempotentCache =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(24, TimeUnit.HOURS)
                    .maximumSize(100_000)
                    .build();

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendBillingMessage(UsageCalculationDTO dto, String userId) {
        org.springframework.messaging.Message<UsageCalculationDTO> message = buildMessage(dto, userId);
        TransactionSendResult result = rocketMQTemplate.sendMessageInTransaction(
                BILLING_DESTINATION,
                message,
                dto.getChatCompletionId()
        );

        if (result.getSendStatus() != SendStatus.SEND_OK) {
            throw new BillingException("消息发送失败，状态: " + result.getSendStatus());
        }
        log.debug("Billing message sent successfully: {}", dto.getChatCompletionId());
    }


    public Mono<Void> sendBillingMessageReactive(UsageCalculationDTO dto, String userId) {
        return Mono.just(dto)
                .publishOn(Schedulers.immediate()) // 禁止切换线程
                .flatMap(d -> {
                    return Mono.fromCallable(() -> { // 在调用线程同步执行
                        org.springframework.messaging.Message<UsageCalculationDTO> message = buildMessage(d, userId);
                        TransactionSendResult result = rocketMQTemplate.sendMessageInTransaction(
                                BILLING_DESTINATION,
                                message,
                                d.getChatCompletionId()
                        );
                        if (result.getSendStatus() != SendStatus.SEND_OK) {
                            throw new BillingException("发送失败");
                        }
                        return result;
                    });
                })
                .then();
    }

    private org.springframework.messaging.Message<UsageCalculationDTO> buildMessage(UsageCalculationDTO dto, String userId) {
        return MessageBuilder.withPayload(dto)
                .setHeader(RocketMQHeaders.KEYS, dto.getChatCompletionId())
                .setHeader("USER_ID", userId)
                .build();
    }

    public void sendChatLogAsync(String messageId,
                                 String requestBody,
                                 ChatResponse response,
                                 String userId,
                                 MQCallback callback) {
        if (idempotentCache.getIfPresent(messageId) != null) {
            log.warn("Message {} already sent, skip duplicate", messageId);
            return;
        }

        try {
            String logData = buildLogMessage(requestBody, response, userId);
            Message msg = new Message(
                    chatLogTopic,
                    "chatLog",
                    messageId, // 关键：设置唯一ID为消息Key
                    logData.getBytes(StandardCharsets.UTF_8)
            );

            producer.send(msg, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    // 标记为已发送
                    idempotentCache.put(messageId, true);
                    if (callback != null) {
                        callback.onSuccess(messageId);
                    }
                }

                @Override
                public void onException(Throwable e) {
                    // 清理状态允许重试
                    idempotentCache.invalidate(messageId);
                    if (callback != null) {
                        callback.onFailure(messageId, e);
                    }
                }
            });
        } catch (Exception e) {
            idempotentCache.invalidate(messageId);
            throw new RuntimeException("消息发送失败", e);
        }
    }

    private String buildLogMessage(String requestBody, ChatResponse response, String userId) {
        return String.format("""
            {
                "timestamp": "%s",
                "userId": "%s",
                "request": %s,
                "response": %s
            }""", LocalDateTime.now(), userId, requestBody, response.getContent());
    }

}