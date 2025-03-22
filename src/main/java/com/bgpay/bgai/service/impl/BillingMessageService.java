package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.bgpay.bgai.exception.BillingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BillingMessageService {
    private final RocketMQTemplate rocketMQTemplate;
    private static final String BILLING_DESTINATION = "BILLING_TOPIC:USER_BILLING";

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendBillingMessage(UsageCalculationDTO dto, String userId) {
        Message<UsageCalculationDTO> message = buildMessage(dto, userId);
        TransactionSendResult result = rocketMQTemplate.sendMessageInTransaction(
                BILLING_DESTINATION,  // 使用包含 tag 的 destination
                message,
                dto.getChatCompletionId()
        );

        if (result.getSendStatus() != SendStatus.SEND_OK) {
            throw new BillingException("消息发送失败，状态: " + result.getSendStatus());
        }
        log.debug("Billing message sent successfully: {}", dto.getChatCompletionId());
    }

    private Message<UsageCalculationDTO> buildMessage(UsageCalculationDTO dto, String userId) {
        return MessageBuilder.withPayload(dto)
                .setHeader(RocketMQHeaders.KEYS, dto.getChatCompletionId())
                .setHeader("USER_ID", userId)
                .build();
    }

    @Recover
    public void recoverSendBillingMessage(Exception e, UsageCalculationDTO dto, String userId) {
        log.error("Billing message send failed after retries. completionId: {}",
                dto.getChatCompletionId(), e);
        // 记录到数据库进行人工干预
        // emergencyService.recordFailedBilling(dto, userId);
    }
}