package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.annotation.PostConstruct;

/**
 * Gateway相关配置禁用器
 * 通过直接注册mock bean定义来替代有问题的Gateway bean
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayBeanPostProcessor implements ApplicationContextAware {
    
    private ConfigurableApplicationContext applicationContext;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }
    
    /**
     * 在容器初始化时注册mock的RouteLocator bean
     */
    @PostConstruct
    public void init() {
        System.out.println("=== 注册mock的RouteLocator bean ===");
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
        
        // 移除可能已存在的bean定义
        if (beanFactory.containsBeanDefinition("customRouteLocator")) {
            beanFactory.removeBeanDefinition("customRouteLocator");
        }
        
        // 注册新的bean定义
        RootBeanDefinition mockRouteLocatorDef = new RootBeanDefinition(RouteLocator.class, () -> Mockito.mock(RouteLocator.class));
        mockRouteLocatorDef.setPrimary(true);
        beanFactory.registerBeanDefinition("customRouteLocator", mockRouteLocatorDef);
    }
} 