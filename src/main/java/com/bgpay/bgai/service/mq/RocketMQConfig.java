package com.bgpay.bgai.service.mq;

import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class RocketMQConfig {

    @Value("${rocketmq.producer.group}")
    private String producerGroup;

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Bean
    public MessageConverter rocketMQMessageConverter() {
        return new MappingJackson2MessageConverter();
    }

    @Bean(name = "transactionExecutor", destroyMethod = "shutdown")
    public ExecutorService transactionExecutor() {
        return Executors.newFixedThreadPool(10, new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("transaction-msg-check-thread-" + this.threadIndex.incrementAndGet());
                return thread;
            }
        });
    }

    @Bean(name = "transactionMQProducer", destroyMethod = "shutdown")
    public TransactionMQProducer transactionMQProducer(ExecutorService transactionExecutor) throws Exception {
        TransactionMQProducer producer = new TransactionMQProducer(producerGroup);
        producer.setNamesrvAddr(nameServer);
        producer.setExecutorService(transactionExecutor);
        producer.setSendMsgTimeout(15000);
        producer.setRetryTimesWhenSendFailed(5);
        producer.setCompressMsgBodyOverHowmuch(1024*4);
        producer.setMaxMessageSize(1024*128);
        producer.setVipChannelEnabled(true);
        producer.start();
        return producer;
    }

    @Bean(destroyMethod = "destroy")
    @Primary
    @DependsOn("transactionMQProducer")
    public RocketMQTemplate rocketMQTemplate(TransactionMQProducer transactionMQProducer, MessageConverter messageConverter) {
        return new CustomRocketMQTemplate(transactionMQProducer, messageConverter);
    }

    /**
     * 自定义RocketMQTemplate，避免重复启动producer
     */
    private static class CustomRocketMQTemplate extends RocketMQTemplate {
        public CustomRocketMQTemplate(TransactionMQProducer producer, MessageConverter messageConverter) {
            super();
            this.setProducer(producer);
            this.setMessageConverter(messageConverter);
        }

        @Override
        public void afterPropertiesSet() {
            // 不执行父类的afterPropertiesSet，避免重复启动producer
        }
    }
} 