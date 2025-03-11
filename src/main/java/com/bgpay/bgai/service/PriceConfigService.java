package com.bgpay.bgai.service;

import com.bgpay.bgai.entity.PriceConfig;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bgpay.bgai.entity.PriceQuery;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author zly
 * @since 2025-03-10 13:30:40
 */
public interface PriceConfigService extends IService<PriceConfig> {

    public void insertOrUpdate(PriceConfig priceConfig);



    public PriceConfig getLatestPrice(String modelType, LocalDateTime time) ;

    public List<PriceConfig> getPriceConfigsByVersion(Integer version);

    PriceConfig findValidPriceConfig(PriceQuery query);
}
