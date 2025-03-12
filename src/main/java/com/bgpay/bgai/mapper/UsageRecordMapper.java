package com.bgpay.bgai.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bgpay.bgai.entity.UsageRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author zly
 * @since 2025-03-09 21:17:29
 */
@Mapper
public interface UsageRecordMapper extends BaseMapper<UsageRecord> {
    default UsageRecord findByCompletionId(String completionId) {
        LambdaQueryWrapper<UsageRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UsageRecord::getChatCompletionId, completionId);
        return this.selectOne(queryWrapper);
    }

    default List<UsageRecord> findByModel(String modelType) {
        // 假设 modelId 是与 modelType 关联的字段
        // 这里需要根据实际的数据库表结构和关联关系进行调整
        // 若存在一个中间表或者其他关联方式，需要修改查询逻辑
        LambdaQueryWrapper<UsageRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UsageRecord::getModelType, modelType);
        return this.selectList(queryWrapper);
    }
}