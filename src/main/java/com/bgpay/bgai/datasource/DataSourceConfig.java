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
        DataSource druid = masterDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(com.alibaba.druid.pool.DruidDataSource.class)
                .build();
        return new DataSourceProxy(druid);
    }

    @Bean(name = "slaveDataSource")
    public DataSource slaveDataSource() {
        validateDriver(slaveDataSourceProperties());
        DataSource druid = slaveDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(com.alibaba.druid.pool.DruidDataSource.class)
                .build();
        return new DataSourceProxy(druid);
    }

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
            Class.forName(properties.getDriverClassName());
            DriverManager.getDriver(properties.getUrl());
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException("Failed to validate JDBC driver", e);
        }
    }
}