package com.bgpay.bgai.datasource;

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

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.dynamic.datasource.master")
    public DataSourceProperties masterDataSourceProperties() {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setGenerateUniqueName(false); // 防止自动生成名称
        return properties;
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.dynamic.datasource.slave")
    public DataSourceProperties slaveDataSourceProperties() {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setGenerateUniqueName(false);
        return properties;
    }

    @Bean(name = "masterDataSource")
    public DataSource masterDataSource() {
        validateDriver(masterDataSourceProperties());
        return masterDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(com.alibaba.druid.pool.DruidDataSource.class)
                .build();
    }

    @Bean(name = "slaveDataSource")
    public DataSource slaveDataSource() {
        validateDriver(slaveDataSourceProperties());
        return slaveDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(com.alibaba.druid.pool.DruidDataSource.class)
                .build();
    }

    @Primary
    @Bean(name = "dynamicDataSource")
    public DataSource dynamicDataSource(
            @Qualifier("masterDataSource") DataSource masterDataSource,
            @Qualifier("slaveDataSource") DataSource slaveDataSource) {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("master", masterDataSource);
        targetDataSources.put("slave", slaveDataSource);
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(masterDataSource);
        return dynamicDataSource;
    }

    private void validateDriver(DataSourceProperties properties) {
        try {
            DriverManager.getDriver(properties.getUrl());
        } catch (SQLException e) {
            throw new IllegalStateException("无法加载JDBC驱动: " + properties.getDriverClassName(), e);
        }
    }
}