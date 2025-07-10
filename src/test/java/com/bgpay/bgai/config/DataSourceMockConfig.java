package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import com.bgpay.bgai.datasource.DynamicDataSource;
import com.bgpay.bgai.datasource.DataSourceType;
import io.seata.rm.datasource.DataSourceProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试环境数据源配置 - 使用H2内存数据库
 * 替代原本的DataSourceConfig, 避免依赖Nacos获取数据库配置
 */
@Configuration
public class DataSourceMockConfig {

    /**
     * 配置主数据源属性
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.dynamic.datasource.master")
    public DataSourceProperties masterDataSourceProperties() {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setDriverClassName("com.mysql.cj.jdbc.Driver");
        properties.setUrl("jdbc:mysql://8.133.246.113:3306/deepseek?autoReconnect=false&useUnicode=true&characterEncoding=UTF-8&characterSetResults=UTF-8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true&useSSL=false");
        properties.setUsername("bgtech");
        properties.setPassword("Zly689258..");
        return properties;
    }

    /**
     * 配置从数据源属性
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.dynamic.datasource.slave")
    public DataSourceProperties slaveDataSourceProperties() {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setDriverClassName("com.mysql.cj.jdbc.Driver");
        properties.setUrl("jdbc:mysql://8.133.246.113:3306/deepseek?autoReconnect=false&useUnicode=true&characterEncoding=UTF-8&characterSetResults=UTF-8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true&useSSL=false");
        properties.setUsername("bgtech");
        properties.setPassword("Zly689258..");
        return properties;
    }

    /**
     * 主数据源
     */
    @Bean(name = "masterDataSource")
    public DataSource masterDataSource() {
        // 创建H2内存数据库
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("masterDb")
                .build();
        
        return new DataSourceProxy(dataSource);
    }

    /**
     * 从数据源
     */
    @Bean(name = "slaveDataSource")
    public DataSource slaveDataSource() {
        // 共用相同的内存数据库
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("slaveDb")
                .build();
        
        return new DataSourceProxy(dataSource);
    }

    /**
     * 动态数据源 - 代替原始DataSourceConfig中的动态数据源
     */
    @Primary
    @Bean(name = "dynamicDataSource")
    public DataSource dynamicDataSource(DataSource masterDataSource, DataSource slaveDataSource) {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceType.MASTER.getValue(), masterDataSource);
        targetDataSources.put(DataSourceType.SLAVE.getValue(), slaveDataSource);
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(masterDataSource);
        dynamicDataSource.afterPropertiesSet();
        return dynamicDataSource;
    }
} 