package com.bgpay.bgai.service;

import com.bgpay.bgai.entity.UsageCalculationDTO;
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

    /**
     * 获取计费数据
     * @param completionId 完成ID
     * @return 计费数据DTO
     */
    UsageCalculationDTO getCalculationDTO(String completionId);

    /**
     * 标记记录为已补偿状态
     * @param completionId 完成ID
     */
    void markAsCompensated(String completionId);

    /**
     * 标记记录为已完成状态
     * @param completionId 完成ID
     */
    void markAsCompleted(String completionId);

    /**
     * 根据完成ID删除记录
     * @param completionId 完成ID
     */
    void deleteByCompletionId(String completionId);
}
