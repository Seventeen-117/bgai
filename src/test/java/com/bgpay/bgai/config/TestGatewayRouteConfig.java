package com.bgpay.bgai.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * 禁用TestGatewayRouteConfig的配置类
 * 在Spring容器初始化时移除TestGatewayRouteConfig相关的bean定义
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TestGatewayRouteConfig implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        // 查找并移除所有与TestGatewayRouteConfig相关的bean定义
        String[] beanNames = registry.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            try {
                // 检查bean定义的来源是否包含TestGatewayRouteConfig
                if (registry.containsBeanDefinition(beanName)) {
                    String source = registry.getBeanDefinition(beanName).getResourceDescription();
                    
                    if (source != null && source.contains("TestGatewayRouteConfig")) {
                        registry.removeBeanDefinition(beanName);
                        System.out.println("移除了来自TestGatewayRouteConfig的bean定义: " + beanName);
                    }
                    
                    // 检查bean类名是否为TestGatewayRouteConfig
                    String className = registry.getBeanDefinition(beanName).getBeanClassName();
                    if (className != null && className.contains("TestGatewayRouteConfig")) {
                        registry.removeBeanDefinition(beanName);
                        System.out.println("移除了TestGatewayRouteConfig类型的bean定义: " + beanName);
                    }
                }
            } catch (Exception e) {
                // 忽略错误，继续处理其他bean
                System.err.println("处理bean定义时出错: " + e.getMessage());
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // 不需要额外处理
    }
} 