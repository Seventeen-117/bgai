package com.bgpay.bgai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.bgpay.bgai.entity.UsageRecord;
import com.bgpay.bgai.entity.UsageInfo;
import com.bgpay.bgai.mapper.UsageRecordMapper;
import com.bgpay.bgai.mapper.UsageInfoMapper;
import com.bgpay.bgai.service.UsageRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author zly
 * @since 2025-03-09 21:17:29
 */
@Slf4j
@Service
public class UsageRecordServiceImpl extends ServiceImpl<UsageRecordMapper, UsageRecord> implements UsageRecordService {

    private final UsageInfoMapper usageInfoMapper;

    public UsageRecordServiceImpl(UsageInfoMapper usageInfoMapper) {
        this.usageInfoMapper = usageInfoMapper;
    }

    @Override
    @Transactional
    public void insertUsageRecord(UsageRecord usageRecord) {
        save(usageRecord);
    }

    @Override
    @Cacheable(value = "usageRecords", key = "#completionId")
    public UsageRecord findByCompletionId(String completionId) {
        LambdaQueryWrapper<UsageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UsageRecord::getChatCompletionId, completionId);
        return getOne(wrapper);
    }

    @Override
    public boolean existsByCompletionId(String chatCompletionId) {
        LambdaQueryWrapper<UsageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UsageRecord::getChatCompletionId, chatCompletionId);
        return count(wrapper) > 0;
    }

    @Override
    @Transactional
    public void batchInsert(List<UsageRecord> records) {
        saveBatch(records);
    }

    @Override
    @Cacheable(value = "usageCalculations", key = "#completionId")
    public UsageCalculationDTO getCalculationDTO(String completionId) {
        UsageRecord record = findByCompletionId(completionId);
        if (record == null) {
            return null;
        }
        
        // 首先获取UsageInfo，因为它包含更详细的token信息
        UsageInfo usageInfo = getUsageInfoByCompletionId(completionId);
        if (usageInfo == null) {
            log.warn("UsageInfo not found for completionId: {}, using record data only", completionId);
        }
        
        UsageCalculationDTO dto = new UsageCalculationDTO();
        // 基本信息
        dto.setChatCompletionId(record.getChatCompletionId());
        dto.setModelType(record.getModelType());
        dto.setCreatedAt(record.getCalculatedAt());
        
        // Token计数 - 优先使用UsageInfo中的详细信息
        if (usageInfo != null) {
            // 缓存相关的token计数
            dto.setPromptCacheHitTokens(usageInfo.getPromptTokensCached()); // 使用缓存的token数
            dto.setPromptCacheMissTokens(usageInfo.getPromptTokens() - usageInfo.getPromptTokensCached()); // 总token数减去缓存的token数
            
            // 完成和推理相关的token计数
            dto.setCompletionTokens(usageInfo.getCompletionTokens());
        } else {
            // 如果没有UsageInfo，使用UsageRecord中的基本信息
            dto.setPromptCacheHitTokens(0);
            dto.setPromptCacheMissTokens(record.getInputTokens());
            dto.setCompletionTokens(record.getOutputTokens());
        }
        
        // 成本计算 - 使用UsageRecord中的成本信息
        dto.setInputCost(record.getInputCost());
        dto.setOutputCost(record.getOutputCost());
        
        return dto;
    }

    /**
     * 根据completionId获取UsageInfo
     */
    private UsageInfo getUsageInfoByCompletionId(String completionId) {
        try {
            return usageInfoMapper.findByCompletionId(completionId);
        } catch (Exception e) {
            log.warn("Failed to get usage info for completionId: {} - {}", completionId, e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"usageRecords", "usageCalculations"}, key = "#completionId")
    public void markAsCompensated(String completionId) {
        UsageRecord record = findByCompletionId(completionId);
        if (record != null) {
            record.setStatus("COMPENSATED");
            record.setUpdatedAt(LocalDateTime.now());
            updateById(record);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"usageRecords", "usageCalculations"}, key = "#completionId")
    public void markAsCompleted(String completionId) {
        UsageRecord record = findByCompletionId(completionId);
        if (record != null) {
            record.setStatus("COMPLETED");
            record.setUpdatedAt(LocalDateTime.now());
            updateById(record);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"usageRecords", "usageCalculations"}, key = "#completionId")
    public void deleteByCompletionId(String completionId) {
        LambdaQueryWrapper<UsageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UsageRecord::getChatCompletionId, completionId);
        remove(wrapper);
    }
}
