package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.entity.UsageInfo;
import com.bgpay.bgai.entity.UsageRecord;
import com.bgpay.bgai.mapper.UsageInfoMapper;
import com.bgpay.bgai.service.UsageInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author zly
 * @since 2025-03-08 23:09:50
 */
@Service
public class UsageInfoServiceImpl extends ServiceImpl<UsageInfoMapper, UsageInfo> implements UsageInfoService {

    @Autowired
    private UsageInfoMapper usageInfoMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void insertUsageInfo(UsageInfo usageInfo) {
        this.save(usageInfo);
    }

    @Override
    public List<UsageInfo> getUsageInfoByIds(List<Long> ids) {
        return usageInfoMapper.selectBatchByIds(ids);
    }

    @Override
    public boolean existsByCompletionId(String chatCompletionId) {
        // 使用 MyBatis-Plus 的 QueryWrapper 构建查询条件
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UsageInfo> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("chat_completion_id", chatCompletionId);
        // 查询满足条件的记录数量
        long count = usageInfoMapper.selectCount(queryWrapper);
        // 如果记录数量大于 0，则表示存在该记录
        return count >0;
    }

}
