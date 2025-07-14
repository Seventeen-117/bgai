package com.bgpay.bgai.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import reactor.core.publisher.Mono;

/**
 * 完全禁用TestGatewayRouteConfig的配置类
 * 使用最高优先级确保在任何其他配置之前执行
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE - 10) // 使用比任何其他配置更高的优先级
public class TestGatewayRouteConfigDisabler {

    /**
     * 创建一个bean工厂后处理器，专门处理TestGatewayRouteConfig相关的bean定义
     * 注意：这是一个静态方法，避免实例化依赖问题
     */
    @Bean
    public static BeanFactoryPostProcessor testGatewayRouteConfigDisablerProcessor() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                if (beanFactory instanceof BeanDefinitionRegistry) {
                    BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
                    
                    // 确保testGatewayRouteConfig bean存在，以避免NoSuchBeanDefinitionException
                    if (!registry.containsBeanDefinition("testGatewayRouteConfig")) {
                        registry.registerBeanDefinition("testGatewayRouteConfig", 
                            BeanDefinitionBuilder.genericBeanDefinition(Object.class).getBeanDefinition());
                        System.out.println("注册了替代的testGatewayRouteConfig bean定义");
                    }
                    
                    // 查找并移除所有与TestGatewayRouteConfig相关的bean定义
                    String[] beanNames = registry.getBeanDefinitionNames();
                    for (String beanName : beanNames) {
                        try {
                            if (beanName.equals("ipKeyResolver") ||
                                beanName.equals("userKeyResolver") ||
                                beanName.equals("apiKeyResolver") ||
                                beanName.equals("customRedisRateLimiter")) {
                                
                                if (registry.containsBeanDefinition(beanName)) {
                                    String source = registry.getBeanDefinition(beanName).getResourceDescription();
                                    if (source != null && 
                                        source.contains("TestGatewayRouteConfig") && 
                                        !source.contains("CompleteGatewayDisablingConfig") &&
                                        !source.contains("TestGatewayRouteConfigDisabler")) {
                                        registry.removeBeanDefinition(beanName);
                                        System.out.println("移除了TestGatewayRouteConfig相关的bean定义: " + beanName);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // 忽略错误，继续处理其他bean
                            System.err.println("处理bean定义时出错: " + e.getMessage());
                        }
                    }
                }
            }
        };
    }
    
    /**
     * 提供一个备用的testGatewayRouteConfig bean，确保它存在
     */
    @Bean(name = "testGatewayRouteConfig")
    public Object testGatewayRouteConfig() {
        return new Object();
    }
    
    /**
     * 提供一个备用的ipKeyResolver bean，确保即使出现问题也能正常运行
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public KeyResolver emergencyIpKeyResolver() {
        return exchange -> Mono.just("emergency-test-ip");
    }
} 