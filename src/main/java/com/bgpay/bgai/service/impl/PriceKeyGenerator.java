package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.entity.PriceQuery;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component("priceKeyGenerator")
public class PriceKeyGenerator implements KeyGenerator {
    // 定义缓存键格式常量（与PriceCacheServiceImpl保持一致）
    private static final String CACHE_KEY_FORMAT = "price:%s:%s:%s:%s";

    @Override
    public Object generate(Object target, Method method, Object... params) {
        // 更正参数类型：PriceQuery（原错误为PriceConfig）
        PriceQuery query = (PriceQuery) params[0];
        return String.format(CACHE_KEY_FORMAT,
                query.getModelType(),
                query.getTimePeriod(),
                query.getCacheStatus(),
                query.getIoType());
    }
}