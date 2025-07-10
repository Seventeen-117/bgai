package com.bgpay.bgai.config;

import com.bgpay.bgai.service.impl.BGAIServiceImpl;
import com.bgpay.bgai.service.mq.MQConsumerService;
import com.bgpay.bgai.service.mq.RocketMQBillingServiceImpl;
import com.bgpay.bgai.service.mq.RocketMQProducerService;
import com.bgpay.bgai.service.UsageRecordService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 直接提供RocketMQBillingServiceImpl的配置类
 * 避免依赖注入问题
 */
@TestConfiguration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DirectRocketMQBillingServiceProvider {

    /**
     * 直接创建并提供RocketMQBillingServiceImpl实例
     * 绕过构造函数注入
     */
    @Bean
    @Primary
    public RocketMQBillingServiceImpl rocketMQBillingServiceImpl() {
        // 创建所需的mock依赖
        RedisTemplate<String, String> mockRedis = Mockito.mock(RedisTemplate.class);
        UsageRecordService mockUsageRecordService = Mockito.mock(UsageRecordService.class);
        RocketMQProducerService mockProducer = Mockito.mock(RocketMQProducerService.class);
        MQConsumerService mockConsumer = Mockito.mock(MQConsumerService.class);
        BGAIServiceImpl mockBGAIService = Mockito.mock(BGAIServiceImpl.class);
        MeterRegistry mockRegistry = new SimpleMeterRegistry();
        
        // 创建一个mock对象
        return Mockito.mock(RocketMQBillingServiceImpl.class);
    }
} 