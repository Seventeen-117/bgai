package com.bgpay.bgai.service;

import com.bgpay.bgai.entity.UsageRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author zly
 * @since 2025-03-09 21:17:29
 */
public interface UsageRecordService extends IService<UsageRecord> {
    public void insertUsageRecord(UsageRecord usageRecord);

    public UsageRecord findByCompletionId(String completionId);

    public boolean existsByCompletionId(@NotBlank String chatCompletionId);

    public void batchInsert(List<UsageRecord> records);


}
