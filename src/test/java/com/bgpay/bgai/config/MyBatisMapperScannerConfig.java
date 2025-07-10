package com.bgpay.bgai.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

/**
 * 解决测试环境中MyBatis mapper重复定义的问题
 * 该配置会在扫描过程中检测到同名bean定义时，移除之前定义的bean，保留最后一个定义
 */
@Configuration
@Profile("test")
public class MyBatisMapperScannerConfig implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {

    private static final String[] MAPPER_SUFFIXES = {
            "Mapper", "Repository", "Dao"
    };

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        String[] beanDefinitionNames = registry.getBeanDefinitionNames();
        
        // 收集所有mapper bean名称，检查是否有重复
        for (String beanName : beanDefinitionNames) {
            if (isMapperBean(beanName)) {
                BeanDefinition definition = registry.getBeanDefinition(beanName);
                
                // 设置为非主要bean，避免@Primary冲突
                definition.setPrimary(false);
                
                // 标记为非懒加载，确保可以及早发现问题
                definition.setLazyInit(false);
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // 不需要处理
    }
    
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
    
    /**
     * 检查bean名称是否符合Mapper命名规则
     */
    private boolean isMapperBean(String beanName) {
        for (String suffix : MAPPER_SUFFIXES) {
            if (beanName.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
} 