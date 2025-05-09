package com.bgpay.bgai.service.mq;


import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class MQConsumerService {
    private final MeterRegistry meterRegistry;
    private final RedisTemplate<String, String> redisTemplate;

    public MQConsumerService(MeterRegistry meterRegistry, RedisTemplate<String, String> redisTemplate) {
        this.meterRegistry = meterRegistry;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 初始化消费者并注册消息监听
     * @param namesrvAddr      nameserver地址
     * @param consumerGroup    消费者组
     * @param topic            主题
     * @param tag              标签
     * @param messageProcessor 消息处理器
     * @param consumeCallback  消费成功回调
     */
    public DefaultMQPushConsumer initConsumer(
            String namesrvAddr,
            String consumerGroup,
            String topic,
            String tag,
            MessageProcessor messageProcessor,
            ConsumeCallback consumeCallback) throws MQClientException {

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(namesrvAddr);
        consumer.setConsumeThreadMin(20);
        consumer.setConsumeThreadMax(50);
        consumer.setConsumeMessageBatchMaxSize(50);
        consumer.setMaxReconsumeTimes(5);
        consumer.subscribe(topic, tag);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);

        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            Map<MessageQueue, Long> offsetMap = new ConcurrentHashMap<>();
            List<MessageExt> successMessages = new ArrayList<>();
            boolean hasFailure = false;

            for (MessageExt msg : msgs) {
                try {
                    messageProcessor.process(msg);

                    successMessages.add(msg);

                    MessageQueue mq = new MessageQueue(msg.getTopic(),
                            msg.getBrokerName(), msg.getQueueId());
                    offsetMap.merge(mq, msg.getQueueOffset(), Math::max);

                } catch (Exception e) {
                    log.error("Message consumption failed [MsgId={}]", msg.getMsgId(), e);
                    hasFailure = true;
                }
            }

            if (hasFailure) {
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }

            // 批量提交offset
            commitOffsets(consumer, offsetMap);

            // 执行回调
            successMessages.forEach(msg -> {
                consumeCallback.onSuccess(msg);
                meterRegistry.counter("message.consumed", "topic", topic).increment();
            });

            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        consumer.start();
        log.info("MQ consumer startup successful [group={}, topic={}, tag={}]", consumerGroup, topic, tag);
        return consumer;
    }

    /**
     * 手动提交offset
     */
    private void commitOffsets(DefaultMQPushConsumer consumer, Map<MessageQueue, Long> offsetMap) {
        offsetMap.forEach((mq, offset) -> {
            try {
                consumer.getOffsetStore().updateOffset(mq, offset + 1, false);
                consumer.getOffsetStore().persist(mq);
            } catch (Exception e) {
                log.error("offset提交失败 [queue={}]", mq, e);
            }
        });
    }

    /**
     * 消息处理接口
     */
    @FunctionalInterface
    public interface MessageProcessor {
        void process(MessageExt message) throws Exception;
    }

    /**
     * 消费成功回调接口
     */
    @FunctionalInterface
    public interface ConsumeCallback {
        void onSuccess(MessageExt message);
    }
}
