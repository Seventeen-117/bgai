package com.bgpay.bgai.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bgpay.bgai.entity.PriceVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 *  价格版本Mapper接口
 * </p>
 *
 * @author zly
 * @since 2025-03-08 23:09:50
 */
@Mapper
public interface PriceVersionMapper extends BaseMapper<PriceVersion> {
    
    /**
     * 获取当前价格版本号
     * @return 当前价格版本号
     */
    default Integer getCurrentVersion() {
        LambdaQueryWrapper<PriceVersion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PriceVersion::getIsCurrent, true)
                   .orderByDesc(PriceVersion::getCreatedAt)
                   .last("LIMIT 1");
        PriceVersion priceVersion = this.selectOne(queryWrapper);
        return priceVersion != null ? priceVersion.getVersion() : null;
    }
    
    /**
     * 获取指定版本的价格版本
     * @param version 版本号
     * @return 价格版本
     */
    default PriceVersion getByVersion(Integer version) {
        LambdaQueryWrapper<PriceVersion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PriceVersion::getVersion, version);
        return this.selectOne(queryWrapper);
    }

    /**
     * 将指定模型的当前价格版本置为无效
     * @param modelId 模型 ID
     * @return 受影响的行数
     */
    default int invalidateCurrentVersion(Integer modelId) {
        // 创建 UpdateWrapper 对象
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<PriceVersion> updateWrapper = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        // 设置更新条件：modelId 相等且 isCurrent 为 true
        updateWrapper.eq("model_id", modelId)
                .eq("is_current", true);
        // 创建 PriceVersion 对象，设置要更新的字段值
        PriceVersion priceVersion = new PriceVersion();
        priceVersion.setIsCurrent(false);
        // 调用 MyBatis-Plus 的 update 方法进行更新操作
        return this.update(priceVersion, updateWrapper);
    }
}
