package com.bgpay.bgai.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bgpay.bgai.entity.PriceConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author zly
 * @since 2025-03-10 13:30:40
 */
@Mapper
public interface PriceConfigMapper extends BaseMapper<PriceConfig> {

    default PriceConfig findLatestPrice(String modelType, LocalDateTime time) {
        QueryWrapper<PriceConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("model_type", modelType)
                .le("effective_time", time)
                .orderByDesc("effective_time")
                .last("LIMIT 1");
        return this.selectOne(queryWrapper);
    }

    default List<PriceConfig> selectByVersion(Integer version) {
        LambdaQueryWrapper<PriceConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PriceConfig::getVersion, version);
        return this.selectList(queryWrapper);
    }


    default PriceConfig findValidPriceConfig(PriceConfig query) {
        LambdaQueryWrapper<PriceConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(PriceConfig::getModelType, query.getModelType())
                .eq(PriceConfig::getTimePeriod, query.getTimePeriod())
                .eq(PriceConfig::getCacheStatus, query.getCacheStatus())
                .eq(PriceConfig::getIoType, query.getIoType())
                .le(PriceConfig::getEffectiveTime, LocalDateTime.now())
                .orderByDesc(PriceConfig::getEffectiveTime)
                .last("LIMIT 1");
        return this.selectOne(queryWrapper);
    }
}
