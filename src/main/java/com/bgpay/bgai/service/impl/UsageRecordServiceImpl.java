package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.entity.UsageRecord;
import com.bgpay.bgai.mapper.UsageRecordMapper;
import com.bgpay.bgai.service.UsageRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author zly
 * @since 2025-03-09 21:17:29
 */
@Service
public class UsageRecordServiceImpl extends ServiceImpl<UsageRecordMapper, UsageRecord> implements UsageRecordService {

    @Autowired
    private UsageRecordMapper usageRecordMapper;

    @Override
    public void insertUsageRecord(UsageRecord usageRecord) {
        this.save(usageRecord);
    }

    @Override
    public UsageRecord findByCompletionId(String completionId) {
        return usageRecordMapper.findByCompletionId(completionId);
    }

    @Override
    public boolean existsByCompletionId(String chatCompletionId) {
        // 使用 MyBatis-Plus 的 QueryWrapper 构建查询条件
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UsageRecord> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("chat_completion_id", chatCompletionId);
        // 查询满足条件的记录数量
        long count = usageRecordMapper.selectCount(queryWrapper);
        // 如果记录数量大于 0，则表示存在该记录
        return count >0;
    }
}
