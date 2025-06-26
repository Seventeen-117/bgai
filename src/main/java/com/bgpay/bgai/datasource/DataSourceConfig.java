package com.bgpay.bgai.datasource;

import io.seata.rm.datasource.DataSourceProxy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据源配置
 * 
 * 配置了主从数据源和动态数据源：
 * - masterDataSource：主数据库连接池
 * - slaveDataSource：从数据库连接池
 * - dynamicDataSource：动态路由数据源，是应用程序的主要数据源（@Primary）
 */
@Configuration
public class DataSourceConfig {

    /**
     * 主数据源属性配置
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.dynamic.datasource.master")
    public DataSourceProperties masterDataSourceProperties() {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setGenerateUniqueName(false); // 防止自动生成名称
        return properties;
    }

    /**
     * 从数据源属性配置
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.dynamic.datasource.slave")
    public DataSourceProperties slaveDataSourceProperties() {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setGenerateUniqueName(false);
        return properties;
    }

    /**
     * 主数据源
     */
    @Bean(name = "masterDataSource")
    public DataSource masterDataSource() {
        validateDriver(masterDataSourceProperties());
        DataSource druid = masterDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(com.alibaba.druid.pool.DruidDataSource.class)
                .build();
        return new DataSourceProxy(druid);
    }

    /**
     * 从数据源
     */
    @Bean(name = "slaveDataSource")
    public DataSource slaveDataSource() {
        validateDriver(slaveDataSourceProperties());
        DataSource druid = slaveDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(com.alibaba.druid.pool.DruidDataSource.class)
                .build();
        return new DataSourceProxy(druid);
    }

    /**
     * 动态数据源 - 这是应用中使用的主要数据源
     */
    @Primary
    @Bean(name = "dynamicDataSource")
    public DataSource dynamicDataSource(
            @Qualifier("masterDataSource") DataSource masterDataSource,
            @Qualifier("slaveDataSource") DataSource slaveDataSource) {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceType.MASTER.getValue(), masterDataSource);
        targetDataSources.put(DataSourceType.SLAVE.getValue(), slaveDataSource);
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(masterDataSource);
        dynamicDataSource.afterPropertiesSet();
        return dynamicDataSource;
    }

    private void validateDriver(DataSourceProperties properties) {
        try {
            if (properties.getDriverClassName() == null) {
                // 尝试使用默认驱动
                properties.setDriverClassName("com.mysql.cj.jdbc.Driver");
                System.out.println("WARNING: 数据源驱动类名为空，使用默认的MySQL驱动");
            }
            
            if (properties.getUrl() == null) {
                throw new RuntimeException("数据库URL为空，请检查Nacos配置是否已正确加载");
            }
            
            Class.forName(properties.getDriverClassName());
            DriverManager.getDriver(properties.getUrl());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("找不到数据库驱动类: " + properties.getDriverClassName(), e);
        } catch (SQLException e) {
            throw new RuntimeException("数据库URL无效: " + properties.getUrl(), e);
        }
    }
}