package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.entity.PriceQuery;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component("priceKeyGenerator")
public class PriceKeyGenerator implements KeyGenerator {
    private static final String CACHE_KEY_FORMAT = "price:%s:%s:%s:%s";

    @Override
    public Object generate(Object target, Method method, Object... params) {
        PriceQuery query = (PriceQuery) params[0];
        return String.format(CACHE_KEY_FORMAT,
                query.getModelType(),
                query.getTimePeriod(),
                query.getCacheStatus(),
                query.getIoType());
    }
}