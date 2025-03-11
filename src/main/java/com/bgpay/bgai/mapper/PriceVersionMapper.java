package com.bgpay.bgai.mapper;

import com.bgpay.bgai.entity.PriceVersion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author zly
 * @since 2025-03-10 15:32:02
 */
@Mapper
public interface PriceVersionMapper extends BaseMapper<PriceVersion> {
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
