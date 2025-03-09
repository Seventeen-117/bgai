package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.entity.UsageInfo;
import com.bgpay.bgai.mapper.UsageInfoMapper;
import com.bgpay.bgai.service.UsageInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void insertUsageInfo(UsageInfo usageInfo) {
        this.save(usageInfo);
    }
}
