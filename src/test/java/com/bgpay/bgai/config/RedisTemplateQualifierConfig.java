package com.bgpay.bgai.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Configuration to resolve RedisTemplate bean conflicts by removing @Primary annotations
 * from conflicting RedisTemplate beans at runtime.
 */
@Configuration
public class RedisTemplateQualifierConfig implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) registry;
        
        // Get all bean names for RedisTemplate
        String[] redisTemplateBeanNames = beanFactory.getBeanNamesForType(RedisTemplate.class);
        
        // Keep track of whether we've found a primary bean
        boolean foundPrimary = false;
        String primaryBeanName = null;
        
        // First pass: identify primary beans
        for (String beanName : redisTemplateBeanNames) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            if (beanDefinition.isPrimary()) {
                if (!foundPrimary) {
                    foundPrimary = true;
                    primaryBeanName = beanName;
                } else {
                    // More than one primary bean found, make this one non-primary
                    beanDefinition.setPrimary(false);
                }
            }
        }
        
        // If no primary bean was found, make one primary
        if (!foundPrimary && redisTemplateBeanNames.length > 0) {
            // Choose the last bean (typically named "redisTemplate" in Spring Boot)
            String lastBeanName = redisTemplateBeanNames[redisTemplateBeanNames.length - 1];
            BeanDefinition beanDefinition = registry.getBeanDefinition(lastBeanName);
            beanDefinition.setPrimary(true);
        }
    }
} 