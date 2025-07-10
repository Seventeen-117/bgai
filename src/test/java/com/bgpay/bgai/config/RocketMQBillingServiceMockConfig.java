package com.bgpay.bgai.config;

import com.bgpay.bgai.service.BillingService;
import com.bgpay.bgai.service.mq.RocketMQBillingServiceImpl;
import com.bgpay.bgai.service.impl.BGAIServiceImpl;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * RocketMQBillingService Mock配置类
 * 提供测试环境使用的BillingService实现
 */
@TestConfiguration
@Order(-2147483647) // Integer.MIN_VALUE + 1, 确保在BGAIServiceMockConfig之后执行
public class RocketMQBillingServiceMockConfig {

    /**
     * 提供RocketMQBillingServiceImpl mock
     * 使用工厂方法避免构造器注入
     */
    @Bean
    public RocketMQBillingServiceImpl rocketMQBillingServiceImpl(BGAIServiceImpl bgaiServiceImpl) {
        // 创建一个特殊的mock，避免调用真实的构造函数
        RocketMQBillingServiceImpl mock = Mockito.mock(RocketMQBillingServiceImpl.class);
        return mock;
    }
    
    /**
     * 提供BillingService mock
     * 不使用@Primary注解，因为BillingServiceMockConfig已经提供了@Primary的BillingService实现
     */
    @Bean
    public BillingService billingService(RocketMQBillingServiceImpl rocketMQBillingServiceImpl) {
        // 返回同一个实例以避免重复
        return rocketMQBillingServiceImpl;
    }
} 