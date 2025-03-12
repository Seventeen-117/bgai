package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.entity.PriceVersion;
import com.bgpay.bgai.mapper.PriceConfigMapper;
import com.bgpay.bgai.mapper.PriceVersionMapper;
import com.bgpay.bgai.service.PriceCacheService;
import com.bgpay.bgai.service.PriceVersionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;



/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author zly
 * @since 2025-03-10 15:32:02
 */
@Service
public class PriceVersionServiceImpl extends ServiceImpl<PriceVersionMapper, PriceVersion> implements PriceVersionService {

    private final PriceVersionMapper priceVersionMapper;
    private final PriceConfigMapper priceConfigMapper;
    private final PriceCacheService priceCacheService;

    public PriceVersionServiceImpl(PriceVersionMapper priceVersionMapper, PriceConfigMapper priceConfigMapper, PriceCacheService priceCacheService) {
        this.priceVersionMapper = priceVersionMapper;
        this.priceConfigMapper = priceConfigMapper;
        this.priceCacheService = priceCacheService;
    }

    @Transactional
    public void switchPriceVersion(PriceVersion newVersion) {
        // 1. 事务内操作
        priceVersionMapper.invalidateCurrentVersion(newVersion.getModelId());
        priceVersionMapper.insert(newVersion);

        // 2. 事务提交后刷新缓存
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        // 按模型刷新缓存，提升性能
                        priceCacheService.refreshCacheByModel(newVersion.getModelType());
                    }
                }
        );
    }

    @Override
    public Integer getCurrentVersion(String modelType) {
        return 0;
    }
}