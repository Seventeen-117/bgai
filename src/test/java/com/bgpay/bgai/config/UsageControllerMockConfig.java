package com.bgpay.bgai.config;

import com.bgpay.bgai.controller.UsageController;
import com.bgpay.bgai.mapper.UsageRecordMapper;
import com.bgpay.bgai.service.BillingService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * UsageController Mock配置类
 * 直接提供UsageController的mock实现，避免依赖注入问题
 */
@TestConfiguration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UsageControllerMockConfig {

    /**
     * 提供RocketMQTemplate的mock实现
     */
    // @Bean
    // @Primary
    // public RocketMQTemplate rocketMQTemplate() {
    //     return Mockito.mock(RocketMQTemplate.class);
    // }
    
    /**
     * 提供UsageRecordMapper的mock实现
     */
    @Bean
    @Primary
    public UsageRecordMapper usageRecordMapper() {
        return Mockito.mock(UsageRecordMapper.class);
    }
    
    /**
     * 直接提供UsageController的mock实现
     * 避免Spring自动创建真实实例
     */
    @Bean
    @Primary
    public UsageController usageController() {
        // 创建mock依赖
        RocketMQTemplate mockTemplate = Mockito.mock(RocketMQTemplate.class);
        BillingService mockBillingService = Mockito.mock(BillingService.class);
        UsageRecordMapper mockMapper = Mockito.mock(UsageRecordMapper.class);
        
        // 使用Mockito创建UsageController的mock
        return Mockito.mock(UsageController.class);
    }
} 