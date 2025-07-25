package com.bgpay.bgai.utils;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;

@Slf4j
public class RocketMQUtils {
    private static final Map<String, DefaultMQPushConsumer> dynamicConsumers = new ConcurrentHashMap<>();
    private static StringRedisTemplate redisTemplate = null;
    private static long redisIdempotentExpireSeconds = 24 * 3600;
    private static String idempotentKeyPrefix = "rocketmq:idempotent:";
    public static void setRedisTemplate(StringRedisTemplate template) {
        redisTemplate = template;
    }
    public static void setRedisIdempotentExpireSeconds(long seconds) {
        redisIdempotentExpireSeconds = seconds;
    }
    public static void setIdempotentKeyPrefix(String prefix) {
        idempotentKeyPrefix = prefix;
    }

    // ========== 生产者 ========== //
    public static SendResult sendSync(DefaultMQProducer producer, String topic, String tag, String key, Object payload) throws Exception {
        Message msg = buildMessage(topic, tag, key, payload);
        return producer.send(msg);
    }

    public static void sendAsync(DefaultMQProducer producer, String topic, String tag, String key, Object payload, SendCallback callback) throws Exception {
        Message msg = buildMessage(topic, tag, key, payload);
        producer.send(msg, callback);
    }

    public static void sendOneway(DefaultMQProducer producer, String topic, String tag, String key, Object payload) throws Exception {
        Message msg = buildMessage(topic, tag, key, payload);
        producer.sendOneway(msg);
    }

    public static SendResult sendOrderly(DefaultMQProducer producer, String topic, String tag, String key, Object payload, String hashKey) throws Exception {
        Message msg = buildMessage(topic, tag, key, payload);
        return producer.send(msg, (mqs, msg1, arg) -> {
            int index = Math.abs(arg.hashCode()) % mqs.size();
            return mqs.get(index);
        }, hashKey);
    }

    /**
     * 发送事务消息，支持 SEATA_XID 属性
     */
    public static void sendTransaction(TransactionMQProducer producer, String topic, String tag, String key, Object payload, Object arg, String seataXid) throws Exception {
        Message msg = buildMessage(topic, tag, key, payload);
        if (seataXid != null) {
            msg.putUserProperty("SEATA_XID", seataXid);
        }
        producer.sendMessageInTransaction(msg, arg);
    }

    private static Message buildMessage(String topic, String tag, String key, Object payload) {
        String body = (payload instanceof String) ? (String) payload : JSON.toJSONString(payload);
        return new Message(topic, tag, key, body.getBytes(StandardCharsets.UTF_8));
    }

    // ========== 消费者 ========== //
    public static void registerConcurrentConsumer(String consumerGroup, String nameServer, String topic, Consumer<MessageExt> handler) throws MQClientException {
        String key = consumerGroup + "-" + topic;
        if (dynamicConsumers.containsKey(key)) return;
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServer);
        consumer.subscribe(topic, "*");
        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                if (isIdempotent(msg)) {
                    log.warn("Duplicate message, skip: {}", msg.getMsgId());
                    continue;
                }
                try {
                    handler.accept(msg);
                } catch (Exception e) {
                    log.error("消费失败，消息进入重试: {}", msg.getMsgId(), e);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();
        dynamicConsumers.put(key, consumer);
        log.info("RocketMQ concurrent consumer started: {}", key);
    }

    public static void registerOrderlyConsumer(String consumerGroup, String nameServer, String topic, Consumer<MessageExt> handler) throws MQClientException {
        String key = consumerGroup + "-" + topic;
        if (dynamicConsumers.containsKey(key)) return;
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServer);
        consumer.subscribe(topic, "*");
        consumer.registerMessageListener((MessageListenerOrderly) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                if (isIdempotent(msg)) {
                    log.warn("Duplicate message, skip: {}", msg.getMsgId());
                    continue;
                }
                try {
                    handler.accept(msg);
                } catch (Exception e) {
                    log.error("顺序消费失败，消息进入重试: {}", msg.getMsgId(), e);
                    return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
                }
            }
            return ConsumeOrderlyStatus.SUCCESS;
        });
        consumer.start();
        dynamicConsumers.put(key, consumer);
        log.info("RocketMQ orderly consumer started: {}", key);
    }

    public static void registerTransactionConsumer(String consumerGroup, String nameServer, String topic, Consumer<MessageExt> handler) throws MQClientException {
        registerConcurrentConsumer(consumerGroup, nameServer, topic, handler);
    }

    private static final Map<String, Boolean> idempotentCache = new ConcurrentHashMap<>();
    private static boolean isIdempotent(MessageExt msg) {
        String key = idempotentKeyPrefix + msg.getMsgId();
        if (redisTemplate != null) {
            ValueOperations<String, String> ops = redisTemplate.opsForValue();
            Boolean exists = ops.setIfAbsent(key, "1", redisIdempotentExpireSeconds, TimeUnit.SECONDS);
            return exists != null && !exists;
        } else {
            if (idempotentCache.putIfAbsent(key, true) != null) {
                return true;
            }
            return false;
        }
    }

    private static String dingtalkWebhookUrl = null;
    public static void setDingtalkWebhookUrl(String url) {
        dingtalkWebhookUrl = url;
    }
    private static void triggerAlarm(MessageExt msg) {
        log.warn("[ALARM] 死信消息告警: msgId={}, topic={}", msg.getMsgId(), msg.getTopic());
        if (dingtalkWebhookUrl != null) {
            try {
                String text = String.format("死信消息告警\nmsgId: %s\ntopic: %s\nbody: %s", msg.getMsgId(), msg.getTopic(), new String(msg.getBody(), StandardCharsets.UTF_8));
                String payload = String.format("{\"msgtype\":\"text\",\"text\":{\"content\":\"%s\"}}", text.replace("\"", "\\\""));
                URL url = new URL(dingtalkWebhookUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.getBytes(StandardCharsets.UTF_8));
                }
                int code = conn.getResponseCode();
                log.info("钉钉告警发送结果: {}", code);
            } catch (Exception e) {
                log.error("钉钉死信告警发送失败", e);
            }
        }
    }

    public static void handleDeadLetter(MessageExt msg) {
        log.error("死信消息处理: msgId={}, topic={}, body={}", msg.getMsgId(), msg.getTopic(), new String(msg.getBody(), StandardCharsets.UTF_8));
        triggerAlarm(msg);
    }

    public static void unregisterConsumer(String consumerGroup, String topic) {
        String key = consumerGroup + "-" + topic;
        DefaultMQPushConsumer consumer = dynamicConsumers.remove(key);
        if (consumer != null) {
            consumer.shutdown();
            log.info("RocketMQ consumer shutdown: {}", key);
        }
    }

    public static <T> T convertMessage(MessageExt msg, Class<T> clazz) {
        String body = new String(msg.getBody(), StandardCharsets.UTF_8);
        return JSON.parseObject(body, clazz);
    }

    /**
     * Seata 事务消息消费端，只有事务状态为 COMMITTED 时才消费
     * seataStatusLookup: xid -> status (如查库返回 "COMMITTED"/"ROLLBACKED"/"RUNNING")
     */
    public static void registerSeataTransactionConsumer(String consumerGroup, String nameServer, String topic, Consumer<MessageExt> handler, java.util.function.Function<String, String> seataStatusLookup) throws MQClientException {
        registerConcurrentConsumer(consumerGroup, nameServer, topic, msg -> {
            String xid = msg.getUserProperty("SEATA_XID");
            if (xid != null && seataStatusLookup != null) {
                String status = seataStatusLookup.apply(xid); // 例如查库返回 "COMMITTED"/"ROLLBACKED"/"RUNNING"
                if (!"COMMITTED".equalsIgnoreCase(status)) {
                    log.warn("Seata 事务未提交，跳过消费: xid={}, status={}", xid, status);
                    return;
                }
            }
            handler.accept(msg);
        });
    }
}