package com.bgpay.bgai.service;

import com.bgpay.bgai.entity.UsageInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author zly
 * @since 2025-03-08 23:09:50
 */
public interface UsageInfoService extends IService<UsageInfo> {
    public void insertUsageInfo(UsageInfo usageInfo);

    public List<UsageInfo> getUsageInfoByIds(List<Long> ids);
}
