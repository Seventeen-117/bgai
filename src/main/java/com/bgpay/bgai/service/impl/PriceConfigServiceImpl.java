package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.entity.PriceConfig;
import com.bgpay.bgai.entity.PriceQuery;
import com.bgpay.bgai.entity.PriceVersion;
import com.bgpay.bgai.mapper.PriceConfigMapper;
import com.bgpay.bgai.mapper.PriceVersionMapper;
import com.bgpay.bgai.priceEnum.ModelType;
import com.bgpay.bgai.priceEnum.PriceType;
import com.bgpay.bgai.priceEnum.TimePeriod;
import com.bgpay.bgai.service.PriceConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author zly
 * @since 2025-03-10 13:30:40
 */
@Service
public class PriceConfigServiceImpl extends ServiceImpl<PriceConfigMapper, PriceConfig> implements PriceConfigService {

    @Autowired
    private PriceConfigMapper priceConfigMapper;
    
    @Autowired
    private PriceVersionMapper priceVersionMapper;

    @Override
    public void insertOrUpdate(PriceConfig priceConfig) {
        this.saveOrUpdate(priceConfig);
    }

    @Override
    public PriceConfig getLatestPrice(String modelType, LocalDateTime time) {
        return priceConfigMapper.findLatestPrice(modelType, time);
    }

    @Override
    public List<PriceConfig> getPriceConfigsByVersion(Integer version) {
        return priceConfigMapper.selectByVersion(version);
    }

    @Override
    public PriceConfig findValidPriceConfig(PriceQuery query) {
        LambdaQueryWrapper<PriceConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PriceConfig::getModelType, query.getModelType())
                .eq(PriceConfig::getTimePeriod, query.getTimePeriod())
                .eq(PriceConfig::getIoType, query.getIoType())
                .le(PriceConfig::getEffectiveTime, LocalDateTime.now());

        if (query.getCacheStatus() != null) {
            queryWrapper.eq(PriceConfig::getCacheStatus, query.getCacheStatus());
        } else {
            queryWrapper.isNull(PriceConfig::getCacheStatus);
        }

        queryWrapper.orderByDesc(PriceConfig::getEffectiveTime)
                .last("LIMIT 1");

        return priceConfigMapper.selectOne(queryWrapper);
    }
    
    @Override
    public BigDecimal getPrice(PriceQuery query) {
        PriceConfig config = findValidPriceConfig(query);
        return config != null ? config.getPrice() : BigDecimal.ZERO;
    }
    
    @Override
    public Integer getCurrentPriceVersion() {
        return priceVersionMapper.getCurrentVersion();
    }
    
    @Override
    public PriceConfig getPriceConfig(ModelType modelType, PriceType priceType, Integer version) {
        return priceConfigMapper.findByModelTypeAndPriceType(modelType.name(), priceType.name(), version);
    }
    
    @Override
    public BigDecimal getPriceByTimePeriod(ModelType modelType, PriceType priceType, TimePeriod timePeriod, Integer version) {
        PriceConfig config = priceConfigMapper.findByModelTypeAndPriceTypeAndTimePeriod(
                modelType.name(), priceType.name(), timePeriod.name(), version);
        return config != null ? config.getPrice() : BigDecimal.ZERO;
    }
    
    @Override
    @Transactional
    public PriceVersion createPriceVersion(PriceVersion priceVersion) {
        priceVersion.setCreateTime(LocalDateTime.now());
        priceVersionMapper.insert(priceVersion);
        return priceVersion;
    }
    
    @Override
    @Transactional
    public PriceConfig updatePriceConfig(PriceConfig priceConfig) {
        priceConfig.setUpdateTime(LocalDateTime.now());
        updateById(priceConfig);
        return priceConfig;
    }
    
    @Override
    @Transactional
    public boolean saveBatchPriceConfigs(List<PriceConfig> priceConfigs) {
        return saveBatch(priceConfigs);
    }
}
