package com.bgpay.bgai.config;

import com.bgpay.bgai.service.impl.BGAIServiceImpl;
import com.bgpay.bgai.service.mq.RocketMQBillingServiceImpl;
import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Field;

/**
 * 测试专用的Bean后处理器
 * 用于在测试环境中处理特殊bean注入
 */
@TestConfiguration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TestBeanPostProcessor {

    /**
     * 创建处理BGAIServiceImpl注入的后处理器
     */
    @Bean
    public BeanPostProcessor bgaiServiceImplInjector() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                return bean;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                // 特殊处理RocketMQBillingServiceImpl
                if (bean instanceof RocketMQBillingServiceImpl) {
                    try {
                        // 获取bgaiService字段
                        Field bgaiServiceField = RocketMQBillingServiceImpl.class.getDeclaredField("bgaiService");
                        bgaiServiceField.setAccessible(true);
                        
                        // 如果字段值为null，注入一个mock
                        if (bgaiServiceField.get(bean) == null) {
                            BGAIServiceImpl mockImpl = Mockito.mock(BGAIServiceImpl.class);
                            bgaiServiceField.set(bean, mockImpl);
                        }
                    } catch (Exception e) {
                        // 如果出错，记录但不打断流程
                        System.err.println("Failed to inject BGAIServiceImpl: " + e.getMessage());
                    }
                }
                return bean;
            }
        };
    }
} 