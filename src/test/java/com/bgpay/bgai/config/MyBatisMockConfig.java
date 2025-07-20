package com.bgpay.bgai.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mockito.Mockito;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 提供MyBatis相关组件的Mock实现，用于测试环境
 */
@Configuration
public class MyBatisMockConfig {

    /**
     * 提供一个Mock的SqlSessionFactory bean，替代原始实现
     * 确保getConfiguration()方法返回非null值
     */
    @Bean
    @Primary
    public SqlSessionFactory sqlSessionFactory() {
        SqlSessionFactory mockFactory = Mockito.mock(SqlSessionFactory.class);
        org.apache.ibatis.session.Configuration mockConfig = Mockito.mock(org.apache.ibatis.session.Configuration.class);
        Mockito.when(mockFactory.getConfiguration()).thenReturn(mockConfig);
        return mockFactory;
    }
    
    /**
     * 提供一个Mock的SqlSessionFactoryBean bean，替代原始实现
     */
    @Bean
    @Primary
    public SqlSessionFactoryBean sqlSessionFactoryBean() {
        return Mockito.mock(SqlSessionFactoryBean.class);
    }

    /**
     * 提供一个Mock的SqlSessionTemplate bean，替代原始实现
     */
    @Bean
    @Primary
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return Mockito.mock(SqlSessionTemplate.class);
    }
} 