package com.bgpay.bgai.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
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
        try {
            LambdaQueryWrapper<UsageRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UsageRecord::getChatCompletionId, completionId);
            return getOne(wrapper);
        } catch (Exception e) {
            log.error("查询记录失败: {}", completionId, e);
            return null;
        }
    }

    @Override
    public boolean existsByCompletionId(String chatCompletionId) {
        try {
            // 使用findByCompletionId来保持一致性
            UsageRecord record = findByCompletionId(chatCompletionId);
            return record != null;
        } catch (Exception e) {
            log.error("检查记录是否存在失败: {}", chatCompletionId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public void batchInsert(List<UsageRecord> records) {
        saveBatch(records);
    }

    @Override
    @Cacheable(value = "usageCalculations", key = "#completionId")
    public UsageCalculationDTO getCalculationDTO(String completionId) {
        try {
            // 首先从UsageInfo获取数据
            UsageInfo usageInfo = getUsageInfoByCompletionId(completionId);
            if (usageInfo == null) {
                log.warn("未找到UsageInfo数据: {}", completionId);
                return null;
            }
            
            UsageCalculationDTO dto = new UsageCalculationDTO();
            dto.setChatCompletionId(usageInfo.getChatCompletionId());
            dto.setModelType(usageInfo.getModelType());
            dto.setCreatedAt(usageInfo.getCreatedAt());
            
            // 设置Token计数
            dto.setPromptCacheHitTokens(usageInfo.getPromptCacheHitTokens());
            dto.setPromptCacheMissTokens(usageInfo.getPromptCacheMissTokens());
            dto.setPromptTokensCached(usageInfo.getPromptTokensCached());
            dto.setCompletionTokens(usageInfo.getCompletionTokens());
            dto.setCompletionReasoningTokens(usageInfo.getCompletionReasoningTokens());
            
            log.info("成功获取计费数据: completionId={}, modelType={}, tokens={}/{}", 
                completionId, dto.getModelType(), dto.getPromptTokens(), dto.getCompletionTokens());
            
            return dto;
        } catch (Exception e) {
            log.error("获取计费数据失败: {}", completionId, e);
            return null;
        }
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
        try {
            UsageRecord record = findByCompletionId(completionId);
            if (record != null) {
                record.setStatus("COMPENSATED");
                record.setUpdatedAt(LocalDateTime.now());
                updateById(record);
                log.info("标记记录为已补偿: {}", completionId);
            } else {
                log.warn("未找到需要标记为已补偿的记录: {}", completionId);
            }
        } catch (Exception e) {
            log.error("标记记录为已补偿失败: {}", completionId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"usageRecords", "usageCalculations"}, key = "#completionId")
    public void markAsCompleted(String completionId) {
        try {
            UsageRecord record = findByCompletionId(completionId);
            if (record != null) {
                record.setStatus("COMPLETED");
                record.setUpdatedAt(LocalDateTime.now());
                updateById(record);
                log.info("标记记录为已完成: {}", completionId);
            } else {
                log.warn("未找到需要标记为已完成的记录: {}", completionId);
            }
        } catch (Exception e) {
            log.error("标记记录为已完成失败: {}", completionId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"usageRecords", "usageCalculations"}, key = "#completionId")
    public void deleteByCompletionId(String completionId) {
        try {
            LambdaQueryWrapper<UsageRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UsageRecord::getChatCompletionId, completionId);
            remove(wrapper);
            log.info("删除记录成功: {}", completionId);
        } catch (Exception e) {
            log.error("删除记录失败: {}", completionId, e);
            throw e;
        }
    }

    @Override
    public boolean update(Wrapper<UsageRecord> updateWrapper) {
        return super.update(updateWrapper);
    }
}
