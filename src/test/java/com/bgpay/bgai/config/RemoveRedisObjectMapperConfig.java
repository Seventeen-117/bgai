package com.bgpay.bgai.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class RemoveRedisObjectMapperConfig {
    @Bean
    public static BeanFactoryPostProcessor removeRedisObjectMapper() {
        return (ConfigurableListableBeanFactory beanFactory) -> {
            if (beanFactory instanceof DefaultListableBeanFactory) {
                DefaultListableBeanFactory dlbf = (DefaultListableBeanFactory) beanFactory;
                if (dlbf.containsBeanDefinition("redisObjectMapper")) {
                    dlbf.removeBeanDefinition("redisObjectMapper");
                }
            }
        };
    }
} 