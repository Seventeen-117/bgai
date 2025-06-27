package com.bgpay.bgai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bgpay.bgai.entity.PriceConfig;
import com.bgpay.bgai.entity.PriceQuery;
import com.bgpay.bgai.entity.PriceVersion;
import com.bgpay.bgai.priceEnum.ModelType;
import com.bgpay.bgai.priceEnum.PriceType;
import com.bgpay.bgai.priceEnum.TimePeriod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  价格配置服务接口
 * </p>
 *
 * @author zly
 * @since 2025-03-08 23:09:50
 */
public interface PriceConfigService extends IService<PriceConfig> {

    /**
     * 根据查询条件获取价格
     * @param query 价格查询条件
     * @return 价格
     */
    BigDecimal getPrice(PriceQuery query);
    
    /**
     * 获取当前价格版本号
     * @return 当前价格版本号
     */
    Integer getCurrentPriceVersion();
    
    /**
     * 根据版本号获取价格配置列表
     * @param version 版本号
     * @return 价格配置列表
     */
    List<PriceConfig> getPriceConfigsByVersion(Integer version);
    
    /**
     * 获取指定模型类型和价格类型的价格配置
     * @param modelType 模型类型
     * @param priceType 价格类型
     * @param version 版本号
     * @return 价格配置
     */
    PriceConfig getPriceConfig(ModelType modelType, PriceType priceType, Integer version);
    
    /**
     * 获取指定模型类型、价格类型和时间段的价格
     * @param modelType 模型类型
     * @param priceType 价格类型
     * @param timePeriod 时间段
     * @param version 版本号
     * @return 价格
     */
    BigDecimal getPriceByTimePeriod(ModelType modelType, PriceType priceType, TimePeriod timePeriod, Integer version);
    
    /**
     * 创建新的价格版本
     * @param priceVersion 价格版本
     * @return 创建的价格版本
     */
    PriceVersion createPriceVersion(PriceVersion priceVersion);
    
    /**
     * 更新价格配置
     * @param priceConfig 价格配置
     * @return 更新后的价格配置
     */
    PriceConfig updatePriceConfig(PriceConfig priceConfig);
    
    /**
     * 批量保存价格配置
     * @param priceConfigs 价格配置列表
     * @return 是否保存成功
     */
    boolean saveBatchPriceConfigs(List<PriceConfig> priceConfigs);

    public void insertOrUpdate(PriceConfig priceConfig);

    public PriceConfig getLatestPrice(String modelType, LocalDateTime time);

    PriceConfig findValidPriceConfig(PriceQuery query);
}
