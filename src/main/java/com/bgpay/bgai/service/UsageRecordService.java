package com.bgpay.bgai.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.bgpay.bgai.entity.UsageRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    public boolean update(Wrapper<UsageRecord> updateWrapper);
    public boolean saveOrUpdate(UsageRecord entity);
    public void updateUsageRecord(UsageRecord entity,String userId);

    UsageRecord findByCompletionId(String completionId);

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
    void markAsCompensated(String completionId, String messageId);

    /**
     * 标记记录为已完成状态
     * @param completionId 完成ID
     */
    void markAsCompleted(String completionId, String messageId);

    /**
     * 根据完成ID删除记录
     * @param completionId 完成ID
     */
    void deleteByCompletionId(String completionId, String messageId);

    void cacheCalculationDTO(String completionId, UsageCalculationDTO dto);

    void updateOrInsertUsageRecord(UsageRecord record);

    // 新增
    UsageRecord findByCompletionIdAndMessageId(String completionId, String messageId);
    
    /**
     * 查询用户的用量记录
     * @param userId 用户ID
     * @param modelType 模型类型（可选）
     * @param startDateTime 开始时间（可选）
     * @param endDateTime 结束时间（可选）
     * @return 用量记录列表
     */
    List<UsageRecord> findUserUsageRecords(String userId, String modelType, 
                                          LocalDateTime startDateTime, LocalDateTime endDateTime);
    
    /**
     * 获取用户的用量汇总信息
     * @param userId 用户ID
     * @param startDateTime 开始时间（可选）
     * @param endDateTime 结束时间（可选）
     * @return 汇总信息
     */
    Map<String, Object> getUserUsageSummary(String userId, LocalDateTime startDateTime, LocalDateTime endDateTime);
    
    /**
     * 获取用户按模型分组的用量统计
     * @param userId 用户ID
     * @param startDateTime 开始时间（可选）
     * @param endDateTime 结束时间（可选）
     * @return 按模型分组的统计
     */
    List<Map<String, Object>> getUserUsageByModel(String userId, LocalDateTime startDateTime, LocalDateTime endDateTime);
    
    /**
     * 获取用户的每日用量趋势
     * @param userId 用户ID
     * @param startDateTime 开始时间（可选）
     * @param endDateTime 结束时间（可选）
     * @return 每日用量趋势
     */
    List<Map<String, Object>> getUserDailyUsageTrend(String userId, LocalDateTime startDateTime, LocalDateTime endDateTime);
    
    /**
     * 获取当前价格配置
     * @return 价格配置信息
     */
    Map<String, Object> getCurrentPriceConfig();
    
    /**
     * 获取系统用量统计
     * @param startDateTime 开始时间（可选）
     * @param endDateTime 结束时间（可选）
     * @return 系统用量统计
     */
    Map<String, Object> getSystemUsageStats(LocalDateTime startDateTime, LocalDateTime endDateTime);
}
