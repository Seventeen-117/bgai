package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Gateway Bean工厂后处理器
 * 在BeanFactory初始化阶段移除和替换Gateway相关bean定义
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor {
    
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        // 移除GatewayRouteConfig的bean定义
        try {
            // 检查bean定义是否存在于registry中
            String[] existingBeanNames = registry.getBeanDefinitionNames();
            System.out.println("注册表中存在的Bean数量: " + existingBeanNames.length);
            
            // 遍历所有bean定义，找出与Gateway相关的
            for (String beanName : existingBeanNames) {
                BeanDefinition def = registry.getBeanDefinition(beanName);
                if (def.getBeanClassName() != null) {
                    String className = def.getBeanClassName();
                    if (className.contains("GatewayRouteConfig") && !className.contains("Test") && 
                        !className.contains("Mock") && !className.contains("Complete")) {
                        System.out.println("发现待移除的Gateway配置类: " + beanName + " -> " + className);
                        // 将该bean定义替换为TestGatewayRouteConfig
                        def.setBeanClassName("com.bgpay.bgai.config.TestGatewayRouteConfig");
                        System.out.println("已将" + beanName + "的类替换为TestGatewayRouteConfig");
                    }
                }
            }
            
            // 确保customRouteLocator bean是我们的mock实现
            RootBeanDefinition mockRouteLocatorDef = new RootBeanDefinition(RouteLocator.class);
            mockRouteLocatorDef.setInstanceSupplier(() -> Mockito.mock(RouteLocator.class));
            mockRouteLocatorDef.setPrimary(true);
            
            // 使用不同的名称避免冲突
            registry.registerBeanDefinition("factoryPostProcessorRouteLocator", mockRouteLocatorDef);
            System.out.println("已注册factoryPostProcessorRouteLocator bean定义");
        } catch (Exception e) {
            System.err.println("处理Gateway相关bean定义时出错: " + e.getMessage());
        }
    }
    
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // 已禁用：避免将bean的className替换为CompleteGatewayDisablingConfig，防止工厂方法指向混乱
        /*
        try {
            for (String beanName : beanFactory.getBeanDefinitionNames()) {
                BeanDefinition def = beanFactory.getBeanDefinition(beanName);
                if (def.getBeanClassName() != null && 
                    def.getBeanClassName().contains("GatewayRouteConfig")) {
                    def.setBeanClassName("com.bgpay.bgai.config.CompleteGatewayDisablingConfig");
                    System.out.println("已修改" + beanName + "的类名为CompleteGatewayDisablingConfig");
                }
            }
        } catch (Exception e) {
            System.err.println("处理BeanFactory时出错: " + e.getMessage());
        }
        */
    }
} 