package com.bgpay.bgai.service;

import com.bgpay.bgai.entity.PriceConfig;
import com.bgpay.bgai.entity.PriceQuery;

public interface PriceCacheService {
    PriceConfig getPriceConfig(PriceQuery query);
    void refreshCache();
    void evictCacheByQuery(PriceQuery query);
    public void refreshCacheByModel(String modelType);
}