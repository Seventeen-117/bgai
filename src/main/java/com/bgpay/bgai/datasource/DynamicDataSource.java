package com.bgpay.bgai.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.lang.Nullable;
import java.util.Map;

public class DynamicDataSource extends AbstractRoutingDataSource {
    
    private boolean initialized = false;
    private Object defaultTargetDataSourceRef; // 新增字段
    
    @Nullable
    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSourceKey();
    }

    @Override
    public void setTargetDataSources(Map<Object, Object> targetDataSources) {
        super.setTargetDataSources(targetDataSources);
        this.initialized = true;
    }

    @Override
    public void setDefaultTargetDataSource(Object defaultTargetDataSource) {
        super.setDefaultTargetDataSource(defaultTargetDataSource);
        this.defaultTargetDataSourceRef = defaultTargetDataSource; // 保存引用
        this.initialized = true;
    }

    @Override
    public void afterPropertiesSet() {
        if (!this.initialized) {
            throw new IllegalStateException("DataSource router not initialized - call setTargetDataSources() and setDefaultTargetDataSource() first");
        }
        super.afterPropertiesSet();
        // 打印实际类型
        System.out.println(">>> DynamicDataSource defaultTargetDataSourceRef type: " + 
            (defaultTargetDataSourceRef == null ? "null" : defaultTargetDataSourceRef.getClass().getName()));
    }

    // 新增：暴露 defaultTargetDataSource 供 SeataResourceManagerListener 获取
    public Object resolveDefaultTargetDataSource() {
        return super.resolveSpecifiedDataSource(this.defaultTargetDataSourceRef);
    }
}