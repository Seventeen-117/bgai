package com.bgpay.bgai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.bgpay.bgai.entity.UsageRecord;
import com.bgpay.bgai.entity.UsageInfo;
import com.bgpay.bgai.mapper.UsageRecordMapper;
import com.bgpay.bgai.mapper.UsageInfoMapper;
import com.bgpay.bgai.service.UsageRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    private static final String CALCULATION_DTO_KEY_PREFIX = "CALCULATION_DTO:";
    private static final int CACHE_EXPIRE_MINUTES = 30;

    private final UsageInfoMapper usageInfoMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public UsageRecordServiceImpl(UsageInfoMapper usageInfoMapper) {
        this.usageInfoMapper = usageInfoMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void updateOrInsertUsageRecord(UsageRecord newRecord) {
        String completionId = newRecord.getChatCompletionId();
        UsageRecord existingRecord = findByCompletionId(completionId);
        
        if (existingRecord != null) {
            // 更新现有记录，累加相关数据
            BigDecimal newInputCost = existingRecord.getInputCost().add(newRecord.getInputCost());
            BigDecimal newOutputCost = existingRecord.getOutputCost().add(newRecord.getOutputCost());
            int newInputTokens = existingRecord.getInputTokens() + newRecord.getInputTokens();
            int newOutputTokens = existingRecord.getOutputTokens() + newRecord.getOutputTokens();
            
            LambdaUpdateWrapper<UsageRecord> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(UsageRecord::getChatCompletionId, completionId)
                    .set(UsageRecord::getInputCost, newInputCost)
                    .set(UsageRecord::getOutputCost, newOutputCost)
                    .set(UsageRecord::getInputTokens, newInputTokens)
                    .set(UsageRecord::getOutputTokens, newOutputTokens)
                    .set(UsageRecord::getUpdatedAt, LocalDateTime.now());
            
            update(updateWrapper);
            log.info("Updated existing usage record for completionId: {}, new input cost: {}, new output cost: {}", 
                    completionId, newInputCost, newOutputCost);
        } else {
            // 插入新记录
            newRecord.setCreatedAt(LocalDateTime.now());
            newRecord.setUpdatedAt(LocalDateTime.now());
            save(newRecord);
            log.info("Inserted new usage record for completionId: {}", completionId);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void insertUsageRecord(UsageRecord record) {
        save(record);
    }

    @Override
    public boolean existsByCompletionId(String completionId) {
        LambdaQueryWrapper<UsageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UsageRecord::getChatCompletionId, completionId);
        return getOne(wrapper) != null;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void batchInsert(List<UsageRecord> records) {
        saveBatch(records);
    }

    @Override
    public UsageCalculationDTO getCalculationDTO(String completionId) {
        String key = CALCULATION_DTO_KEY_PREFIX + completionId;
        String json = redisTemplate.opsForValue().get(key);
        
        if (json == null) {
            log.warn("未找到UsageInfo数据: {}", completionId);
            return null;
        }
        
        try {
            return JSON.parseObject(json, UsageCalculationDTO.class);
        } catch (Exception e) {
            log.error("解析UsageInfo数据失败: {}", completionId, e);
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
    @Transactional(propagation = Propagation.REQUIRED)
    @CacheEvict(value = {"usageRecords", "usageCalculations"}, key = "#completionId")
    public void markAsCompensated(String completionId) {
        LambdaUpdateWrapper<UsageRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UsageRecord::getChatCompletionId, completionId)
                .set(UsageRecord::getStatus, "COMPENSATED")
                .set(UsageRecord::getUpdatedAt, LocalDateTime.now());
        update(wrapper);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    @CacheEvict(value = {"usageRecords", "usageCalculations"}, key = "#completionId")
    public void markAsCompleted(String completionId) {
        LambdaUpdateWrapper<UsageRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UsageRecord::getChatCompletionId, completionId)
                .set(UsageRecord::getStatus, "COMPLETED")
                .set(UsageRecord::getUpdatedAt, LocalDateTime.now());
        update(wrapper);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    @CacheEvict(value = {"usageRecords", "usageCalculations"}, key = "#completionId")
    public void deleteByCompletionId(String completionId) {
        LambdaQueryWrapper<UsageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UsageRecord::getChatCompletionId, completionId);
        remove(wrapper);
    }

    @Override
    public boolean update(Wrapper<UsageRecord> updateWrapper) {
        return super.update(updateWrapper);
    }

    @Override
    public void updateUsageRecord(UsageRecord entity,String userId) {
        LambdaQueryWrapper<UsageRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UsageRecord::getUserId, userId);
        UsageRecord record = this.baseMapper.selectOne(queryWrapper);

        if (record == null) {
            throw new RuntimeException("未找到 userId=" + userId + " 的记录");
        }
        record.setInputCost(entity.getInputCost());
        record.setOutputCost(entity.getOutputCost());
        record.setPriceVersion(entity.getPriceVersion());
        record.setCalculatedAt(entity.getCalculatedAt());
        record.setMessageId(entity.getMessageId());
        record.setInputTokens(entity.getInputTokens());
        record.setOutputTokens(entity.getOutputTokens());
        record.setStatus(entity.getStatus());
        record.setUpdatedAt(LocalDateTime.now()); // 更新时间
        this.baseMapper.updateById(record);
    }

    @Override
    public void cacheCalculationDTO(String completionId, UsageCalculationDTO dto) {
        String key = CALCULATION_DTO_KEY_PREFIX + completionId;
        String json = JSON.toJSONString(dto);
        redisTemplate.opsForValue().set(key, json, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        log.info("缓存计费数据成功: {}", completionId);
    }

    @Override
    public UsageRecord findByCompletionId(String completionId) {
        LambdaQueryWrapper<UsageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UsageRecord::getChatCompletionId, completionId)
               .last("LIMIT 1");
        return getOne(wrapper);
    }

    // 新增
    @Override
    public List<UsageRecord> findAllByCompletionId(String completionId) {
        LambdaQueryWrapper<UsageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UsageRecord::getChatCompletionId, completionId);
        return list(wrapper);
    }

    // 新增
    @Override
    public UsageRecord findByCompletionIdAndMessageId(String completionId, String messageId) {
        LambdaQueryWrapper<UsageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UsageRecord::getChatCompletionId, completionId)
               .eq(UsageRecord::getMessageId, messageId)
               .last("LIMIT 1");
        return getOne(wrapper);
    }
}
