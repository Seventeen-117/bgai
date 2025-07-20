package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.core.Ordered;
import reactor.core.publisher.Mono;

/**
 * 综合Gateway禁用配置
 * 将所有禁用Gateway的配置集中在一个类中，便于管理
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // 确保比任何其他配置优先级更高
@EnableAutoConfiguration(exclude = {
    GatewayAutoConfiguration.class,
    org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration.class,
    org.springframework.cloud.gateway.config.GatewayMetricsAutoConfiguration.class
})
@ComponentScan(excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        com.bgpay.bgai.config.GatewayRouteConfig.class,
        com.bgpay.bgai.config.GatewayRouteConfigMock.class
    })
})
public class CompleteGatewayDisablingConfig {
    
    /**
     * 提供一个mock的RouteLocatorBuilder
     */
    @Bean
    @Primary
    public RouteLocatorBuilder routeLocatorBuilder() {
        RouteLocatorBuilder mockBuilder = Mockito.mock(RouteLocatorBuilder.class);
        RouteLocatorBuilder.Builder mockRouteBuilder = Mockito.mock(RouteLocatorBuilder.Builder.class);
        
        // 设置routes()方法返回非null值
        Mockito.when(mockBuilder.routes()).thenReturn(mockRouteBuilder);
        
        // 设置build()方法返回mock RouteLocator
        RouteLocator mockRouteLocator = Mockito.mock(RouteLocator.class);
        Mockito.when(mockRouteBuilder.build()).thenReturn(mockRouteLocator);
        
        return mockBuilder;
    }
    
    /**
     * 直接提供一个mock的customRouteLocator
     */
    @Bean(name = "customRouteLocator")
    @Primary
    public RouteLocator customRouteLocator() {
        return Mockito.mock(RouteLocator.class);
    }
    
    /**
     * Mock的IP限流解析器
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just("test-ip");
    }
    
    /**
     * Mock的用户限流解析器
     */
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.just("test-user");
    }
    
    /**
     * Mock的API限流解析器
     */
    @Bean
    @Primary
    public KeyResolver apiKeyResolver() {
        return exchange -> Mono.just("test-api");
    }
    
    /**
     * Mock的Redis限流器
     */
    @Bean
    @Primary
    public RedisRateLimiter customRedisRateLimiter() {
        return Mockito.mock(RedisRateLimiter.class);
    }
    
    /**
     * Mock的ConfigurationService
     * 解决GatewayRedisAutoConfiguration依赖注入失败问题
     */
    @Bean
    @Primary
    public ConfigurationService configurationService() {
        return Mockito.mock(ConfigurationService.class);
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
    
    /**
     * 添加备份的bean工厂后处理器
     * 使用不同的名称避免与TestGatewayRouteConfigDisabler冲突
     */
    @Bean(name = "completeDisablingConfigProcessor")
    public static BeanFactoryPostProcessor completeDisablingConfigProcessor() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                if (beanFactory instanceof BeanDefinitionRegistry) {
                    BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
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
                                    if (source != null && source.contains("TestGatewayRouteConfig")) {
                                        registry.removeBeanDefinition(beanName);
                                        System.out.println("CompleteGatewayDisablingConfig: 移除了TestGatewayRouteConfig相关的bean定义: " + beanName);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // 忽略错误，继续处理其他bean
                            System.err.println("CompleteGatewayDisablingConfig: 处理bean定义时出错: " + e.getMessage());
                        }
                    }
                }
            }
        };
    }
    
    /**
     * 兜底：防止Spring找不到 testGatewayRouteConfigDisablerProcessor
     */
    @Bean
    public static BeanFactoryPostProcessor testGatewayRouteConfigDisablerProcessor() {
        // 返回一个空实现，什么都不做
        return beanFactory -> {};
    }
    
    /**
     * 系统启动时打印日志，确认配置被加载
     */
    public CompleteGatewayDisablingConfig() {
        System.out.println("=== CompleteGatewayDisablingConfig has been loaded ===");
        System.out.println("=== Gateway functionality should be completely disabled ===");
    }
    
    /**
     * 添加一个标记bean，避免MockGatewayBean相关错误
     */
    @Bean
    public String gatewayDisableMarker() {
        return "Gateway configuration completely disabled for tests";
    }
    
    /**
     * 提供一个备用的testGatewayRouteConfig bean，使用不同的名称
     */
    @Bean(name = "completeDisablingTestGatewayRouteConfig")
    @Primary
    public Object completeDisablingTestGatewayRouteConfig() {
        return new Object();
    }
} 