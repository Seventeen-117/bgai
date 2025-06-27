package com.bgpay.bgai.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bgpay.bgai.entity.PriceConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  价格配置Mapper接口
 * </p>
 *
 * @author zly
 * @since 2025-03-08 23:09:50
 */
@Mapper
public interface PriceConfigMapper extends BaseMapper<PriceConfig> {

    /**
     * 查找特定模型类型在特定时间之前的最新价格配置
     * @param modelType 模型类型
     * @param time 时间点
     * @return 价格配置
     */
    default PriceConfig findLatestPrice(String modelType, LocalDateTime time) {
        LambdaQueryWrapper<PriceConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PriceConfig::getModelType, modelType)
                   .le(PriceConfig::getEffectiveTime, time)
                   .orderByDesc(PriceConfig::getEffectiveTime)
                   .last("LIMIT 1");
        return this.selectOne(queryWrapper);
    }

    /**
     * 根据版本号查询价格配置
     * @param version 版本号
     * @return 价格配置列表
     */
    default List<PriceConfig> selectByVersion(Integer version) {
        LambdaQueryWrapper<PriceConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PriceConfig::getVersion, version);
        return this.selectList(queryWrapper);
    }
    
    /**
     * 根据模型类型和价格类型查询价格配置
     * @param modelType 模型类型
     * @param priceType 价格类型
     * @param version 版本号
     * @return 价格配置
     */
    default PriceConfig findByModelTypeAndPriceType(String modelType, String priceType, Integer version) {
        LambdaQueryWrapper<PriceConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PriceConfig::getModelType, modelType)
                   .eq(PriceConfig::getIoType, priceType)
                   .eq(PriceConfig::getVersion, version)
                   .last("LIMIT 1");
        return this.selectOne(queryWrapper);
    }
    
    /**
     * 根据模型类型、价格类型和时间段查询价格配置
     * @param modelType 模型类型
     * @param priceType 价格类型
     * @param timePeriod 时间段
     * @param version 版本号
     * @return 价格配置
     */
    default PriceConfig findByModelTypeAndPriceTypeAndTimePeriod(String modelType, String priceType, String timePeriod, Integer version) {
        LambdaQueryWrapper<PriceConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PriceConfig::getModelType, modelType)
                   .eq(PriceConfig::getIoType, priceType)
                   .eq(PriceConfig::getTimePeriod, timePeriod)
                   .eq(PriceConfig::getVersion, version)
                   .last("LIMIT 1");
        return this.selectOne(queryWrapper);
    }
    
    /**
     * 获取所有模型类型
     * @return 模型类型列表
     */
    default List<String> getAllModelTypes() {
        QueryWrapper<PriceConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("DISTINCT model_type");
        return this.selectObjs(queryWrapper)
                .stream()
                .map(Object::toString)
                .distinct()
                .toList();
    }
    
    /**
     * 获取指定模型的所有价格类型
     * @param modelType 模型类型
     * @return 价格类型列表
     */
    default List<String> getPriceTypesByModel(String modelType) {
        QueryWrapper<PriceConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("DISTINCT io_type")
                   .eq("model_type", modelType);
        return this.selectObjs(queryWrapper)
                .stream()
                .map(Object::toString)
                .toList();
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
