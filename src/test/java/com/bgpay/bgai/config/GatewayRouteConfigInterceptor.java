package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Ordered;

/**
 * Gateway路由配置拦截器
 * 直接注册一个完全替换原始GatewayRouteConfig的bean
 */
// @Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayRouteConfigInterceptor {
    
    /**
     * 直接使用相同的bean名称替换原始bean
     */
    @Bean(name = "gatewayRouteConfig")
    @Primary
    public Object gatewayRouteConfig() {
        // 返回一个空的对象，只为了占位
        return new Object();
    }
    
    /**
     * 提供一个完整替换customRouteLocator的bean
     */
    @Bean(name = "customRouteLocator")
    @Primary
    public RouteLocator customRouteLocator() {
        System.out.println("加载替代的customRouteLocator bean");
        return Mockito.mock(RouteLocator.class);
    }
    
    /**
     * 注册Bean工厂后处理器，处理可能已经存在的bean定义
     * 注意：使用不同的bean名称避免冲突
     * 标记为静态方法，避免实例依赖问题
     */
    @Bean(name = "interceptorGatewayConfigProcessor")
    public static BeanFactoryPostProcessor interceptorGatewayConfigProcessor() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                // 尝试完全重定义GatewayRouteConfig相关的bean定义
                if (beanFactory instanceof BeanDefinitionRegistry) {
                    BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
                    
                    try {
                        // 移除现有的定义
                        if (registry.containsBeanDefinition("gatewayRouteConfig")) {
                            registry.removeBeanDefinition("gatewayRouteConfig");
                            System.out.println("移除了gatewayRouteConfig bean定义");
                        }
                        
                        // 注册一个简单的替代定义
                        registry.registerBeanDefinition("gatewayRouteConfig", 
                            BeanDefinitionBuilder.genericBeanDefinition(Object.class).getBeanDefinition());
                        System.out.println("注册了替代的gatewayRouteConfig bean定义");
                        
                    } catch (Exception e) {
                        System.err.println("处理Gateway bean定义时出错: " + e);
                    }
                }
            }
        };
    }
} 