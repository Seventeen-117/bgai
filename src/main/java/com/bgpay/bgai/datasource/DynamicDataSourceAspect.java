package com.bgpay.bgai.datasource;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class DynamicDataSourceAspect {

    @Before("@annotation(DS)")
    public void beforeSwitchDataSource(JoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        DS dsAnnotation = method.getAnnotation(DS.class);
        if (dsAnnotation != null) {
            DataSourceContextHolder.setDataSourceKey(dsAnnotation.value());
        }
    }

    @After("@annotation(DS)")
    public void afterSwitchDataSource() {
        DataSourceContextHolder.clearDataSourceKey();
    }
}