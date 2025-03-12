package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.entity.PriceConfig;
import com.bgpay.bgai.entity.PriceQuery;
import com.bgpay.bgai.mapper.PriceConfigMapper;
import com.bgpay.bgai.service.PriceConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
}
