package com.bgpay.bgai.service.mq;

import com.alibaba.fastjson2.JSON;
import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.bgpay.bgai.response.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RocketMQProducerService {
    private static final String BILLING_DESTINATION = "BILLING_TOPIC:USER_BILLING";

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${rocketmq.producer.group}")
    private String producerGroup;

    @Value("${rocketmq.producer.send-message-timeout:3000}")
    private int sendMessageTimeout;

    @Value("${rocketmq.producer.retry-times-when-send-failed:2}")
    private int retryTimesWhenSendFailed;

    @Value("${rocketmq.topic.chat-log}")
    private String chatLogTopic;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    private DefaultMQProducer producer;
    
    private final Cache<String, Boolean> idempotentCache = CacheBuilder.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();

    @PostConstruct
    public void init() {
        log.info("Initializing RocketMQProducerService with producer group: {}, nameServer: {}", producerGroup, nameServer);
        producer = new DefaultMQProducer();
        producer.setProducerGroup(producerGroup);
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(sendMessageTimeout);
        producer.setRetryTimesWhenSendFailed(retryTimesWhenSendFailed);
        
        try {
            producer.start();
            log.info("RocketMQ producer started successfully");
        } catch (MQClientException e) {
            log.error("Failed to start RocketMQ producer", e);
            throw new RuntimeException("Failed to start RocketMQ producer", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (producer != null) {
            producer.shutdown();
            log.info("RocketMQ producer shutdown");
        }
    }

    public SendResult sendMessage(String topic, String tags, String keys, Object message) throws Exception {
        if (producer == null) {
            throw new RuntimeException("RocketMQ producer not initialized");
        }

        String jsonMessage = new ObjectMapper().writeValueAsString(message);
        Message msg = new Message(topic, tags, keys, jsonMessage.getBytes(StandardCharsets.UTF_8));
        
        try {
            SendResult sendResult = producer.send(msg);
            log.info("Message sent successfully, msgId: {}", sendResult.getMsgId());
            return sendResult;
        } catch (Exception e) {
            log.error("Failed to send message", e);
            throw e;
        }
    }

    public SendResult sendChatLogMessage(String keys, Object message) throws Exception {
        return sendMessage(chatLogTopic, "chat_log", keys, message);
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendBillingMessage(UsageCalculationDTO dto, String userId) {
        try {
            // 直接用 JSON 字节流
            byte[] payload = JSON.toJSONBytes(dto);
            org.springframework.messaging.Message<byte[]> message = MessageBuilder
                .withPayload(payload)
                .setHeader("USER_ID", userId)
                .build();
            rocketMQTemplate.sendMessageInTransaction(
                BILLING_DESTINATION,
                message,
                dto.getChatCompletionId()
            );
            log.info("Successfully sent billing message in transaction, completionId: {}", dto.getChatCompletionId());
        } catch (Exception e) {
            log.error("Failed to send billing message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send billing message", e);
        }
    }

    public Mono<Void> sendBillingMessageReactive(UsageCalculationDTO dto, String userId) {
        return Mono.fromRunnable(() -> sendBillingMessage(dto, userId));
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
                    messageId,
                    logData.getBytes(StandardCharsets.UTF_8)
            );

            producer.send(msg, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    idempotentCache.put(messageId, true);
                    if (callback != null) {
                        callback.onSuccess(messageId);
                    }
                }

                @Override
                public void onException(Throwable e) {
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
    
    // 新增重载方法，支持lambda表达式直接回调
    public void sendChatLogAsync(String messageId,
                                 String requestBody,
                                 ChatResponse response,
                                 String userId,
                                 java.util.function.Consumer<String> onSuccess,
                                 java.util.function.BiConsumer<String, Throwable> onFailure) {
        if (idempotentCache.getIfPresent(messageId) != null) {
            log.warn("Message {} already sent, skip duplicate", messageId);
            return;
        }

        try {
            String logData = buildLogMessage(requestBody, response, userId);
            Message msg = new Message(
                    chatLogTopic,
                    "chatLog",
                    messageId,
                    logData.getBytes(StandardCharsets.UTF_8)
            );

            producer.send(msg, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    idempotentCache.put(messageId, true);
                    if (onSuccess != null) {
                        onSuccess.accept(messageId);
                    }
                }

                @Override
                public void onException(Throwable e) {
                    idempotentCache.invalidate(messageId);
                    if (onFailure != null) {
                        onFailure.accept(messageId, e);
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