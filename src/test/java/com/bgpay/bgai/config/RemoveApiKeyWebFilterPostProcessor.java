package com.bgpay.bgai.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class RemoveApiKeyWebFilterPostProcessor {
    @Bean
    public static BeanFactoryPostProcessor removeApiKeyWebFilter() {
        return (ConfigurableListableBeanFactory beanFactory) -> {
            String[] names = beanFactory.getBeanNamesForType(com.bgpay.bgai.filter.ApiKeyWebFilter.class, false, false);
            if (beanFactory instanceof DefaultListableBeanFactory) {
                DefaultListableBeanFactory dlbf = (DefaultListableBeanFactory) beanFactory;
                for (String name : names) {
                    dlbf.removeBeanDefinition(name);
                }
            }
        };
    }
} 