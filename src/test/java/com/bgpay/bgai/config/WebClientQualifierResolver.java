package com.bgpay.bgai.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;

/**
 * WebClient主键冲突解决器
 * 解决多个带有@Primary注解的WebClient bean冲突问题
 * 通过bean定义后处理器动态修改bean定义
 */
@TestConfiguration(proxyBeanMethods = false)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebClientQualifierResolver {

    /**
     * 创建一个BeanDefinitionRegistryPostProcessor，在容器初始化过程中解决主键冲突
     */
    @Bean
    public static BeanDefinitionRegistryPostProcessor webClientPrimaryResolver() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
                // 不在此方法中实现，而是在postProcessBeanFactory中处理
            }

            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                // 处理WebClient的主键冲突
                resolveWebClientPrimaryConflict(beanFactory);
                
                // 处理WebClient.Builder的主键冲突
                resolveWebClientBuilderPrimaryConflict(beanFactory);
            }
            
            private void resolveWebClientPrimaryConflict(ConfigurableListableBeanFactory beanFactory) {
                // 获取所有WebClient类型的bean名称
                String[] beanNames = beanFactory.getBeanNamesForType(WebClient.class);
                
                // 检查是否有多个primary bean
                List<String> primaryBeans = Arrays.stream(beanNames)
                        .filter(name -> {
                            BeanDefinition bd = beanFactory.getBeanDefinition(name);
                            return bd.isPrimary();
                        })
                        .toList();
                
                // 如果有多个primary bean，则只保留一个作为primary
                if (primaryBeans.size() > 1) {
                    // 保留指定的webClient作为primary
                    String keepPrimaryBean = "webClient";
                    
                    for (String beanName : primaryBeans) {
                        if (!beanName.equals(keepPrimaryBean)) {
                            BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
                            bd.setPrimary(false);
                        }
                    }
                }
            }
            
            private void resolveWebClientBuilderPrimaryConflict(ConfigurableListableBeanFactory beanFactory) {
                try {
                    // 获取所有WebClient.Builder类型的bean名称
                    String[] builderBeanNames = beanFactory.getBeanNamesForType(WebClient.Builder.class);
                    
                    // 检查是否有多个primary bean
                    List<String> primaryBuilderBeans = Arrays.stream(builderBeanNames)
                            .filter(name -> {
                                try {
                                    BeanDefinition bd = beanFactory.getBeanDefinition(name);
                                    return bd.isPrimary();
                                } catch (Exception e) {
                                    return false;
                                }
                            })
                            .toList();
                    
                    // 如果有多个primary bean，则只保留一个作为primary
                    if (primaryBuilderBeans.size() > 1) {
                        // 保留webClientBuilder作为唯一的primary
                        String keepPrimaryBean = "webClientBuilder";
                        
                        for (String beanName : primaryBuilderBeans) {
                            if (!beanName.equals(keepPrimaryBean)) {
                                BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
                                bd.setPrimary(false);
                            }
                        }
                    }
                } catch (Exception e) {
                    // 捕获可能的异常，确保不影响应用启动
                    System.err.println("处理WebClient.Builder主键冲突时出错: " + e.getMessage());
                }
            }
        };
    }
} 