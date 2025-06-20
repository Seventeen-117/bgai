package com.bgpay.bgai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.bgpay.bgai.entity.UsageInfo;
import com.bgpay.bgai.entity.UsageRecord;
import com.bgpay.bgai.mapper.UsageInfoMapper;
import com.bgpay.bgai.service.UsageInfoService;
import com.bgpay.bgai.service.UsageRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bgpay.bgai.entity.PriceConfig;
import com.bgpay.bgai.entity.PriceQuery;
import com.bgpay.bgai.exception.BillingException;
import com.bgpay.bgai.service.PriceCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;

import java.time.*;
import java.util.List;

import static com.bgpay.bgai.entity.PriceConstants.OUTPUT_TYPE;

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
    private static final Logger log = LoggerFactory.getLogger(UsageInfoServiceImpl.class);
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalTime DISCOUNT_START = LocalTime.of(0, 30);
    private static final LocalTime DISCOUNT_END = LocalTime.of(8, 30);

    @Autowired
    private UsageInfoMapper usageInfoMapper;

    @Autowired
    private UsageRecordService usageRecordService;

    @Autowired
    private PriceCacheService priceCache;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void insertUsageInfo(UsageInfo usageInfo) {
        save(usageInfo);
    }

    @Override
    public List<UsageInfo> getUsageInfoByIds(List<Long> ids) {
        return usageInfoMapper.selectBatchByIds(ids);
    }

    @Override
    public boolean existsByCompletionId(String chatCompletionId) {
        LambdaQueryWrapper<UsageInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UsageInfo::getChatCompletionId, chatCompletionId);
        return count(wrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean processUsageInfo(UsageCalculationDTO dto, String userId) {
        // 计算总的输入和输出tokens，处理可能为null的情况
        int totalInputTokens = (dto.getPromptCacheHitTokens() != null ? dto.getPromptCacheHitTokens() : 0) + 
                             (dto.getPromptCacheMissTokens() != null ? dto.getPromptCacheMissTokens() : 0);
        int totalOutputTokens = dto.getCompletionTokens() != null ? dto.getCompletionTokens() : 0;

        try {
            log.info("Processing usage info for user: {}, completionId: {}", userId, dto.getChatCompletionId());
            
            // 检查是否已经处理过
            UsageInfo existingInfo = usageInfoMapper.selectByCompletionId(dto.getChatCompletionId());
            if (existingInfo != null) {
                log.info("Usage info already exists for completionId: {}, updating...", dto.getChatCompletionId());
                // 更新现有记录
                int newInputTokens = existingInfo.getPromptTokens() + totalInputTokens;
                int newOutputTokens = existingInfo.getCompletionTokens() + totalOutputTokens;
                int newCacheHitTokens = existingInfo.getPromptCacheHitTokens() + 
                    (dto.getPromptCacheHitTokens() != null ? dto.getPromptCacheHitTokens() : 0);
                int newCacheMissTokens = existingInfo.getPromptCacheMissTokens() + 
                    (dto.getPromptCacheMissTokens() != null ? dto.getPromptCacheMissTokens() : 0);
                int newReasoningTokens = existingInfo.getCompletionReasoningTokens() + 
                    (dto.getCompletionReasoningTokens() != null ? dto.getCompletionReasoningTokens() : 0);
                
                existingInfo.setPromptTokens(newInputTokens);
                existingInfo.setCompletionTokens(newOutputTokens);
                existingInfo.setTotalTokens(newInputTokens + newOutputTokens);
                existingInfo.setPromptCacheHitTokens(newCacheHitTokens);
                existingInfo.setPromptCacheMissTokens(newCacheMissTokens);
                existingInfo.setPromptTokensCached(dto.getPromptTokensCached());
                existingInfo.setCompletionReasoningTokens(newReasoningTokens);
                existingInfo.setUpdatedAt(LocalDateTime.now());
                usageInfoMapper.updateById(existingInfo);
                return true;
            }

            // 创建新的使用信息记录
            UsageInfo usageInfo = new UsageInfo();
            usageInfo.setUserId(userId);
            usageInfo.setChatCompletionId(dto.getChatCompletionId());
            usageInfo.setModelType(dto.getModelType());
            usageInfo.setPromptTokens(totalInputTokens);
            usageInfo.setCompletionTokens(totalOutputTokens);
            usageInfo.setTotalTokens(totalInputTokens + totalOutputTokens);
            usageInfo.setPromptCacheHitTokens(dto.getPromptCacheHitTokens() != null ? dto.getPromptCacheHitTokens() : 0);
            usageInfo.setPromptCacheMissTokens(dto.getPromptCacheMissTokens() != null ? dto.getPromptCacheMissTokens() : 0);
            usageInfo.setPromptTokensCached(dto.getPromptTokensCached());
            usageInfo.setCompletionReasoningTokens(dto.getCompletionReasoningTokens() != null ? dto.getCompletionReasoningTokens() : 0);
            usageInfo.setCreatedAt(LocalDateTime.now());
            usageInfo.setUpdatedAt(LocalDateTime.now());

            // 插入新记录
            usageInfoMapper.insert(usageInfo);
            log.info("Successfully processed usage info for completionId: {}", dto.getChatCompletionId());
            
            return true;
        } catch (DuplicateKeyException e) {
            // 处理并发情况下可能出现的重复插入
            log.warn("Duplicate record detected for completionId: {}, retrying update...", 
                    dto.getChatCompletionId());
            try {
                // 重试更新现有记录
                UsageInfo existingInfo = usageInfoMapper.selectByCompletionId(dto.getChatCompletionId());
                if (existingInfo != null) {
                    int newInputTokens = existingInfo.getPromptTokens() + totalInputTokens;
                    int newOutputTokens = existingInfo.getCompletionTokens() + totalOutputTokens;
                    int newCacheHitTokens = existingInfo.getPromptCacheHitTokens() + 
                        (dto.getPromptCacheHitTokens() != null ? dto.getPromptCacheHitTokens() : 0);
                    int newCacheMissTokens = existingInfo.getPromptCacheMissTokens() + 
                        (dto.getPromptCacheMissTokens() != null ? dto.getPromptCacheMissTokens() : 0);
                    int newReasoningTokens = existingInfo.getCompletionReasoningTokens() + 
                        (dto.getCompletionReasoningTokens() != null ? dto.getCompletionReasoningTokens() : 0);
                    
                    existingInfo.setPromptTokens(newInputTokens);
                    existingInfo.setCompletionTokens(newOutputTokens);
                    existingInfo.setTotalTokens(newInputTokens + newOutputTokens);
                    existingInfo.setPromptCacheHitTokens(newCacheHitTokens);
                    existingInfo.setPromptCacheMissTokens(newCacheMissTokens);
                    existingInfo.setPromptTokensCached(dto.getPromptTokensCached());
                    existingInfo.setCompletionReasoningTokens(newReasoningTokens);
                    existingInfo.setUpdatedAt(LocalDateTime.now());
                    usageInfoMapper.updateById(existingInfo);
                    return true;
                }
            } catch (Exception retryEx) {
                log.error("Failed to update existing record on retry: {}", retryEx.getMessage(), retryEx);
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to process usage info for completionId: {}", 
                    dto.getChatCompletionId(), e);
            return false;
        }
    }

    private Integer getPriceVersion(UsageCalculationDTO dto, String timePeriod) {
        // Create a price query object for output
        PriceQuery query = new PriceQuery(
                dto.getModelType(),
                timePeriod,
                null,
                OUTPUT_TYPE
        );

        PriceConfig config = priceCache.getPriceConfig(query);

        if (config == null) {
            throw new BillingException("Price config not found");
        } else if (!(config instanceof PriceConfig)) {
            log.error("refresh Cache config by ModelType: {}", dto.getModelType());
            priceCache.refreshCacheByModel(dto.getModelType());
        }
        Integer cachedVersion = config.getVersion();
        log.info("Output price config priceVersion: {}", cachedVersion);
        return cachedVersion;
    }

    private ZonedDateTime convertToBeijingTime(LocalDateTime utcTime) {
        return utcTime.atZone(ZoneOffset.UTC)
                .withZoneSameInstant(BEIJING_ZONE);
    }

    private String determineTimePeriod(ZonedDateTime beijingTime) {
        LocalDate date = beijingTime.toLocalDate();

        ZonedDateTime discountStart = ZonedDateTime.of(date, DISCOUNT_START, BEIJING_ZONE);
        ZonedDateTime discountEnd = ZonedDateTime.of(date, DISCOUNT_END, BEIJING_ZONE);

        if (discountEnd.isBefore(discountStart)) {
            discountEnd = discountEnd.plusDays(1);
        }

        return (beijingTime.isAfter(discountStart) && beijingTime.isBefore(discountEnd))
                ? "discount" : "standard";
    }
}
