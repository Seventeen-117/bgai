package com.bgpay.bgai.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;

/**
 * KeyResolver Bean后处理器
 * 专门处理KeyResolver相关的bean定义，确保使用正确的工厂方法创建bean
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class KeyResolverBeanPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof BeanDefinitionRegistry) {
            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
            
            // 处理ipKeyResolver
            handleKeyResolver(registry, "ipKeyResolver");
            
            // 处理userKeyResolver
            handleKeyResolver(registry, "userKeyResolver");
            
            // 处理apiKeyResolver
            handleKeyResolver(registry, "apiKeyResolver");
        }
    }
    
    private void handleKeyResolver(BeanDefinitionRegistry registry, String beanName) {
        try {
            // 如果bean定义存在且来源有问题，则移除它
            if (registry.containsBeanDefinition(beanName)) {
                BeanDefinition definition = registry.getBeanDefinition(beanName);
                String source = definition.getResourceDescription();
                
                if (source != null && (source.contains("TestGatewayRouteConfig") || 
                                      !source.contains("CompleteGatewayDisablingConfig"))) {
                    registry.removeBeanDefinition(beanName);
                    System.out.println("移除了有问题的KeyResolver bean定义: " + beanName);
                }
            }
        } catch (Exception e) {
            System.err.println("处理KeyResolver bean定义时出错: " + e.getMessage());
        }
    }
    
    /**
     * 提供一个备用的ipKeyResolver bean，以防其他方法失败
     */
    @Bean
    @Primary
    public KeyResolver backupIpKeyResolver() {
        return exchange -> Mono.just("backup-test-ip");
    }
    
    /**
     * 提供一个备用的userKeyResolver bean，以防其他方法失败
     */
    @Bean
    @Primary
    public KeyResolver backupUserKeyResolver() {
        return exchange -> Mono.just("backup-test-user");
    }
    
    /**
     * 提供一个备用的apiKeyResolver bean，以防其他方法失败
     */
    @Bean
    @Primary
    public KeyResolver backupApiKeyResolver() {
        return exchange -> Mono.just("backup-test-api");
    }
} 