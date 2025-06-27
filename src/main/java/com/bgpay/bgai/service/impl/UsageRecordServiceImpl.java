package com.bgpay.bgai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bgpay.bgai.entity.PriceConfig;
import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.bgpay.bgai.entity.UsageRecord;
import com.bgpay.bgai.entity.UsageInfo;
import com.bgpay.bgai.mapper.UsageRecordMapper;
import com.bgpay.bgai.mapper.UsageInfoMapper;
import com.bgpay.bgai.service.PriceCacheService;
import com.bgpay.bgai.service.PriceConfigService;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private final UsageRecordMapper usageRecordMapper;
    private final PriceCacheService priceCacheService;
    private final PriceConfigService priceConfigService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public UsageRecordServiceImpl(UsageInfoMapper usageInfoMapper,
                                 UsageRecordMapper usageRecordMapper,
                                 PriceCacheService priceCacheService,
                                 PriceConfigService priceConfigService) {
        this.usageInfoMapper = usageInfoMapper;
        this.usageRecordMapper = usageRecordMapper;
        this.priceCacheService = priceCacheService;
        this.priceConfigService = priceConfigService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void updateOrInsertUsageRecord(UsageRecord newRecord) {
        String completionId = newRecord.getChatCompletionId();
        String messageId = newRecord.getMessageId();
        UsageRecord existingRecord = findByCompletionIdAndMessageId(completionId, messageId);
        if (existingRecord != null) {
            log.info("Usage record already exists for completionId: {}, messageId: {}, skip insert (幂等)", completionId, messageId);
            return;
        }
        newRecord.setCreatedAt(LocalDateTime.now());
        newRecord.setUpdatedAt(LocalDateTime.now());
        try {
            save(newRecord);
            log.info("Inserted new usage record for completionId: {}, messageId: {}", completionId, messageId);
        } catch (Exception e) {
            log.info("Duplicate usage record for completionId: {}, messageId: {}, skip insert (幂等)", completionId, messageId);
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
    @CacheEvict(value = {"usageRecords", "usageCalculations"}, key = "#completionId + ':' + #messageId")
    public void markAsCompensated(String completionId, String messageId) {
        LambdaUpdateWrapper<UsageRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UsageRecord::getChatCompletionId, completionId)
                .eq(UsageRecord::getMessageId, messageId)
                .set(UsageRecord::getStatus, "COMPENSATED")
                .set(UsageRecord::getUpdatedAt, LocalDateTime.now());
        update(wrapper);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    @CacheEvict(value = {"usageRecords", "usageCalculations"}, key = "#completionId + ':' + #messageId")
    public void markAsCompleted(String completionId, String messageId) {
        LambdaUpdateWrapper<UsageRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UsageRecord::getChatCompletionId, completionId)
                .eq(UsageRecord::getMessageId, messageId)
                .set(UsageRecord::getStatus, "COMPLETED")
                .set(UsageRecord::getUpdatedAt, LocalDateTime.now());
        update(wrapper);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    @CacheEvict(value = {"usageRecords", "usageCalculations"}, key = "#completionId + ':' + #messageId")
    public void deleteByCompletionId(String completionId, String messageId) {
        LambdaQueryWrapper<UsageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UsageRecord::getChatCompletionId, completionId)
               .eq(UsageRecord::getMessageId, messageId);
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

    // 新增
    @Override
    public UsageRecord findByCompletionIdAndMessageId(String completionId, String messageId) {
        LambdaQueryWrapper<UsageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UsageRecord::getChatCompletionId, completionId)
               .eq(UsageRecord::getMessageId, messageId)
               .last("LIMIT 1");
        return getOne(wrapper);
    }

    @Override
    public List<UsageRecord> findUserUsageRecords(String userId, String modelType,
                                                 LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return usageRecordMapper.findUserUsageRecords(userId, modelType, startDateTime, endDateTime);
    }
    
    @Override
    public Map<String, Object> getUserUsageSummary(String userId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Map<String, Object> summary = new HashMap<>();
        
        // 获取用户总消费
        BigDecimal totalCost = usageRecordMapper.getUserTotalCost(userId, startDateTime, endDateTime);
        summary.put("totalCost", totalCost != null ? totalCost : BigDecimal.ZERO);
        
        // 获取总输入和输出token数
        Integer totalInputTokens = usageRecordMapper.getUserTotalInputTokens(userId, startDateTime, endDateTime);
        Integer totalOutputTokens = usageRecordMapper.getUserTotalOutputTokens(userId, startDateTime, endDateTime);
        summary.put("totalInputTokens", totalInputTokens != null ? totalInputTokens : 0);
        summary.put("totalOutputTokens", totalOutputTokens != null ? totalOutputTokens : 0);
        
        // 获取总请求次数
        Long totalRequests = usageRecordMapper.getUserTotalRequests(userId, startDateTime, endDateTime);
        summary.put("totalRequests", totalRequests != null ? totalRequests : 0);
        
        // 获取最近一次使用时间
        LocalDateTime lastUsageTime = usageRecordMapper.getUserLastUsageTime(userId);
        summary.put("lastUsageTime", lastUsageTime != null ? 
                lastUsageTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        
        return summary;
    }
    
    @Override
    public List<Map<String, Object>> getUserUsageByModel(String userId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return usageRecordMapper.getUserUsageByModel(userId, startDateTime, endDateTime);
    }
    
    @Override
    public List<Map<String, Object>> getUserDailyUsageTrend(String userId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return usageRecordMapper.getUserDailyUsageTrend(userId, startDateTime, endDateTime);
    }
    
    @Override
    public Map<String, Object> getCurrentPriceConfig() {
        Map<String, Object> priceConfig = new HashMap<>();
        
        // 获取当前价格版本
        Integer currentVersion = priceConfigService.getCurrentPriceVersion();
        priceConfig.put("currentVersion", currentVersion);
        
        // 获取所有模型的价格配置
        List<PriceConfig> configs = priceConfigService.getPriceConfigsByVersion(currentVersion);
        
        // 将价格配置按模型类型分组
        Map<String, List<PriceConfig>> pricesByModel = configs.stream()
                .collect(Collectors.groupingBy(PriceConfig::getModelType));
        
        priceConfig.put("pricesByModel", pricesByModel);
        
        return priceConfig;
    }
    
    @Override
    public Map<String, Object> getSystemUsageStats(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Map<String, Object> systemStats = new HashMap<>();
        
        // 获取系统总消费
        BigDecimal totalSystemCost = usageRecordMapper.getSystemTotalCost(startDateTime, endDateTime);
        systemStats.put("totalCost", totalSystemCost != null ? totalSystemCost : BigDecimal.ZERO);
        
        // 获取系统总输入和输出token数
        Integer totalSystemInputTokens = usageRecordMapper.getSystemTotalInputTokens(startDateTime, endDateTime);
        Integer totalSystemOutputTokens = usageRecordMapper.getSystemTotalOutputTokens(startDateTime, endDateTime);
        systemStats.put("totalInputTokens", totalSystemInputTokens != null ? totalSystemInputTokens : 0);
        systemStats.put("totalOutputTokens", totalSystemOutputTokens != null ? totalSystemOutputTokens : 0);
        
        // 获取系统总请求次数
        Long totalSystemRequests = usageRecordMapper.getSystemTotalRequests(startDateTime, endDateTime);
        systemStats.put("totalRequests", totalSystemRequests != null ? totalSystemRequests : 0);
        
        // 获取活跃用户数
        Long activeUsers = usageRecordMapper.getActiveUserCount(startDateTime, endDateTime);
        systemStats.put("activeUsers", activeUsers != null ? activeUsers : 0);
        
        // 获取按模型分组的系统用量
        List<Map<String, Object>> systemUsageByModel = usageRecordMapper.getSystemUsageByModel(startDateTime, endDateTime);
        systemStats.put("usageByModel", systemUsageByModel);
        
        // 获取系统每日用量趋势
        List<Map<String, Object>> systemDailyTrend = usageRecordMapper.getSystemDailyUsageTrend(startDateTime, endDateTime);
        systemStats.put("dailyTrend", systemDailyTrend);
        
        return systemStats;
    }
}
