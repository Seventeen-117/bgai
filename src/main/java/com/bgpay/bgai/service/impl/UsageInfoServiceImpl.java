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
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean processUsageInfo(UsageCalculationDTO dto, String userId) {
        try {
            // 检查是否已处理
            if (existsByCompletionId(dto.getChatCompletionId())) {
                log.info("Usage info already processed for completionId: {}", dto.getChatCompletionId());
                return true;
            }

            // 确定时间段
            ZonedDateTime beijingTime = convertToBeijingTime(dto.getCreatedAt());
            String timePeriod = determineTimePeriod(beijingTime);

            // 创建使用记录
            UsageRecord record = new UsageRecord();
            record.setUserId(userId);
            record.setChatCompletionId(dto.getChatCompletionId());
            record.setModelType(dto.getModelType());
            record.setInputTokens(dto.getPromptCacheHitTokens() + dto.getPromptCacheMissTokens());
            record.setOutputTokens(dto.getCompletionTokens());
            record.setInputCost(dto.getInputCost());
            record.setOutputCost(dto.getOutputCost());
            record.setCalculatedAt(dto.getCreatedAt());
            record.setStatus("PENDING");
            record.setCreatedAt(LocalDateTime.now());

            // 获取价格版本
            try {
                Integer priceVersion = getPriceVersion(dto, timePeriod);
                record.setPriceVersion(priceVersion);
            } catch (BillingException e) {
                log.warn("Failed to get price version, using default version 1: {}", e.getMessage());
                record.setPriceVersion(1);
            }

            // 保存使用记录
            usageRecordService.insertUsageRecord(record);

            // 创建使用信息
            UsageInfo usageInfo = new UsageInfo();
            usageInfo.setChatCompletionId(dto.getChatCompletionId());
            usageInfo.setModelType(dto.getModelType());
            usageInfo.setPromptTokens(dto.getPromptCacheHitTokens() + dto.getPromptCacheMissTokens());
            usageInfo.setCompletionTokens(dto.getCompletionTokens());
            usageInfo.setTotalTokens(usageInfo.getPromptTokens() + usageInfo.getCompletionTokens());
            usageInfo.setPromptCacheHitTokens(dto.getPromptCacheHitTokens());
            usageInfo.setPromptCacheMissTokens(dto.getPromptCacheMissTokens());
            usageInfo.setPromptTokensCached(dto.getPromptTokensCached());
            usageInfo.setCompletionReasoningTokens(dto.getCompletionReasoningTokens());
            usageInfo.setCreatedAt(LocalDateTime.now());

            // 保存使用信息
            insertUsageInfo(usageInfo);

            return true;
        } catch (Exception e) {
            log.error("Failed to process usage info for completionId: {}", dto.getChatCompletionId(), e);
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
