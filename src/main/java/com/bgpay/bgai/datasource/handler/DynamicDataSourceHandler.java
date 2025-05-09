package com.bgpay.bgai.datasource.handler;

import com.bgpay.bgai.datasource.DataSourceContextHolder;
import com.bgpay.bgai.datasource.DataSourceType;
import com.bgpay.bgai.datasource.annotation.DataSource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Objects;

@Aspect
@Order(1)
@Component
public class DynamicDataSourceHandler {
    private static final Logger log = LoggerFactory.getLogger(DynamicDataSourceHandler.class);

    @Pointcut("@annotation(com.bgpay.bgai.datasource.annotation.DataSource)"
            + "|| @within(com.bgpay.bgai.datasource.annotation.DataSource)")
    public void dsPointCut() {
    }

    @Around("dsPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        DataSource dataSource = getDataSource(point);

        if (Objects.nonNull(dataSource)) {
            DataSourceContextHolder.setDataSourceKey(dataSource.value().getValue());
            log.debug("Set data source to: {}", dataSource.value().getValue());
        }

        try {
            return point.proceed();
        } finally {
            DataSourceContextHolder.clearDataSourceKey();
            log.debug("Clear data source");
        }
    }

    private DataSource getDataSource(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        DataSource dataSource = AnnotationUtils.findAnnotation(method, DataSource.class);
        if (Objects.nonNull(dataSource)) {
            return dataSource;
        }

        return AnnotationUtils.findAnnotation(signature.getDeclaringType(), DataSource.class);
    }
} 