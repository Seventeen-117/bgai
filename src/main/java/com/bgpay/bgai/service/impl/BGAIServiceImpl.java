package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.bgpay.bgai.entity.UsageRecord;
import com.bgpay.bgai.service.UsageRecordService;
import com.bgpay.bgai.service.UsageInfoService;
import com.bgpay.bgai.service.mq.RocketMQProducerService;
import com.bgpay.bgai.service.PriceCacheService;
import com.bgpay.bgai.entity.PriceConfig;
import com.bgpay.bgai.entity.PriceQuery;
import com.bgpay.bgai.exception.BillingException;
import com.bgpay.bgai.service.BGAIService;
import com.bgpay.bgai.utils.TimeZoneUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

import static com.bgpay.bgai.entity.PriceConstants.*;

/**
 * BGAI服务实现，用于Saga状态机
 * 包含正向操作和补偿操作
 */
@Slf4j
@Service
public class BGAIServiceImpl implements BGAIService {
    
    private static final Logger logger = LoggerFactory.getLogger(BGAIServiceImpl.class);
    private static final String PROCESSED_KEY_PREFIX = "BILLING:PROCESSED:";
    private static final String CALCULATION_DTO_KEY_PREFIX = "CALCULATION_DTO:";
    private static final String INPUT_TYPE = "input";
    private static final String OUTPUT_TYPE = "output";
    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");

    
    @Autowired
    private UsageRecordService usageRecordService;
    
    @Autowired
    private UsageInfoService usageInfoService;
    
    @Autowired
    private PriceCacheService priceCache;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    /**
     * 执行第一步操作：准备处理环境
     */
    @Transactional
    public boolean executeFirstStep(String businessKey) {
        try {
            logger.info("执行第一步操作 - 准备处理环境, businessKey: {}", businessKey);
            String[] parts = businessKey.split(":");
            String userId = parts[0];
            String completionId = parts[1];
            
            // 检查Redis状态并清理可能存在的旧状态
            String redisKey = PROCESSED_KEY_PREFIX + completionId;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                logger.warn("发现旧的处理状态，执行清理: {}", redisKey);
                redisTemplate.delete(redisKey);
                redisTemplate.delete(redisKey + ":processing");
                redisTemplate.delete(redisKey + ":processed");
            }
            
            // 设置处理中状态
            redisTemplate.opsForValue().set(redisKey + ":processing", "1", 30, TimeUnit.MINUTES);
            logger.info("成功设置处理中状态: {}", redisKey + ":processing");
            
            return true;
        } catch (Exception e) {
            logger.error("准备处理环境失败, businessKey: {}, error: {}", businessKey, e.getMessage(), e);
            throw new BillingException("准备处理环境失败: " + e.getMessage());
        }
    }
    
    /**
     * 补偿第一步操作：回滚账单消息发送
     */
    @Transactional
    public boolean compensateFirstStep(String businessKey) {
        try {
            logger.info("补偿第一步操作 - 回滚账单消息, businessKey: {}", businessKey);
            String[] parts = businessKey.split(":");
            String completionId = parts[1];
            
            // 清除处理中状态
            String redisKey = PROCESSED_KEY_PREFIX + completionId;
            redisTemplate.delete(redisKey + ":processing");
            
            // 标记消息为已补偿状态
            usageRecordService.markAsCompensated(completionId, null);
            return true;
        } catch (Exception e) {
            logger.error("补偿账单消息失败, businessKey: {}", businessKey, e);
            return false;
        }
    }

    /**
     * 执行第二步操作：处理账单消息，插入使用记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean executeSecondStep(String businessKey, boolean firstStepResult) {
        if (!firstStepResult) {
            logger.error("First step failed, skipping second step. businessKey: {}", businessKey);
            return false;
        }

        try {
            String[] parts = businessKey.split(":");
            String userId = parts[0];
            String completionId = parts[1];

            // 获取缓存的计费数据
            UsageCalculationDTO dto = usageRecordService.getCalculationDTO(completionId);
            if (dto == null) {
                logger.error("计费数据不存在, businessKey: {}", businessKey);
                return false;
            }

            // 获取当前北京时间
            ZonedDateTime beijingTime = TimeZoneUtils.getCurrentBeijingTime();
            String timePeriod = TimeZoneUtils.determineTimePeriod(beijingTime);

            // 计算输入和输出成本
            BigDecimal inputCost = calculateInputCost(dto, timePeriod);
            BigDecimal outputCost = calculateOutputCost(dto, timePeriod);

            // 创建使用记录
            UsageRecord record = new UsageRecord();
            record.setUserId(userId);
            record.setChatCompletionId(completionId);
            record.setModelType(dto.getModelType());
            record.setInputCost(inputCost);
            record.setOutputCost(outputCost);
            
            // 防止null值异常
            int promptCacheHitTokens = dto.getPromptCacheHitTokens() != null ? dto.getPromptCacheHitTokens() : 0;
            int promptCacheMissTokens = dto.getPromptCacheMissTokens() != null ? dto.getPromptCacheMissTokens() : 0;
            Integer completionTokens = dto.getCompletionTokens() != null ? dto.getCompletionTokens() : 0;
            
            record.setInputTokens(promptCacheHitTokens + promptCacheMissTokens);
            record.setOutputTokens(completionTokens);
            record.setStatus("PENDING");
            record.setPriceVersion(getPriceVersion(dto, timePeriod));
            record.setMessageId(dto.getMessageId());

            // 更新或插入使用记录
            usageRecordService.updateOrInsertUsageRecord(record);

            logger.info("Second step completed successfully. businessKey: {}", businessKey);
            return true;
        } catch (Exception e) {
            logger.error("Second step failed. businessKey: {}, error: {}", businessKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 执行第三步操作：更新账单状态，完成最终处理
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean executeThirdStep(String businessKey, boolean secondStepResult, String messageId) {
        if (!secondStepResult) {
            log.error("Second step failed, skipping third step. businessKey: {}", businessKey);
            return false;
        }

        try {
            log.info("Executing third step - updating billing status, businessKey: {}", businessKey);
            
            String[] parts = businessKey.split(":");
            String userId = parts[0];
            String completionId = parts[1];

            // 获取使用记录
            UsageRecord record = usageRecordService.findByCompletionIdAndMessageId(completionId, messageId);
            if (record == null) {
                log.error("Usage record not found for completionId: {}, messageId: {}", completionId, messageId);
                return false;
            }

            // 检查记录状态
            if ("COMPLETED".equals(record.getStatus())) {
                log.info("Record already marked as completed: {}, messageId={}, status={}", completionId, messageId, record.getStatus());
                return true;
            }

            // 获取计费数据
            UsageCalculationDTO dto = usageRecordService.getCalculationDTO(completionId);
            if (dto == null) {
                log.error("Billing data not found for completionId: {}", completionId);
                return false;
            }

            try {
                // 更新用户使用信息
                boolean usageUpdateSuccess = usageInfoService.processUsageInfo(dto, userId);
                if (!usageUpdateSuccess) {
                    log.error("Failed to update usage info for user: {}, completionId: {}", userId, completionId);
                    return false;
                }

                // 标记记录为已完成
                usageRecordService.markAsCompleted(completionId, messageId);
                
                // 清理缓存的计费数据
                String cacheKey = CALCULATION_DTO_KEY_PREFIX + completionId;
                redisTemplate.delete(cacheKey);

                log.info("Third step completed successfully. businessKey: {}", businessKey);
                return true;
                
            } catch (Exception e) {
                log.error("Error processing usage info: userId={}, completionId={}, error={}",
                        userId, completionId, e.getMessage(), e);
                return false;
            }

        } catch (Exception e) {
            log.error("Third step failed. businessKey: {}, error: {}", businessKey, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 补偿第二步操作：回滚账单处理
     */
    @Transactional
    public boolean compensateSecondStep(String businessKey) {
        try {
            logger.info("补偿第二步操作 - 回滚账单处理, businessKey: {}", businessKey);
            String[] parts = businessKey.split(":");
            String completionId = parts[1];
            
            // 检查记录是否存在
            if (!usageRecordService.existsByCompletionId(completionId)) {
                logger.info("记录不存在，无需补偿, completionId: {}", completionId);
                // 清除处理状态
                String redisKey = PROCESSED_KEY_PREFIX + completionId;
                redisTemplate.delete(redisKey + ":processed");
                redisTemplate.delete(redisKey + ":processing");
                return true;
            }
            
            // 删除使用记录
            usageRecordService.deleteByCompletionId(completionId, null);
            
            // 清除处理状态
            String redisKey = PROCESSED_KEY_PREFIX + completionId;
            redisTemplate.delete(redisKey + ":processed");
            redisTemplate.delete(redisKey + ":processing");
            
            return true;
        } catch (Exception e) {
            logger.error("补偿账单处理失败, businessKey: {}", businessKey, e);
            return false;
        }
    }
    
    /**
     * 补偿第三步操作：回滚账单状态
     */
    @Transactional
    public boolean compensateThirdStep(String businessKey, String messageId) {
        try {
            logger.info("补偿第三步操作 - 回滚账单状态, businessKey: {}", businessKey);
            String[] parts = businessKey.split(":");
            String completionId = parts[1];
            
            // 检查记录是否存在
            if (usageRecordService.findByCompletionIdAndMessageId(completionId, messageId) == null) {
                logger.info("记录不存在，无需补偿, completionId: {}, messageId: {}", completionId, messageId);
                // 清除所有状态
                String redisKey = PROCESSED_KEY_PREFIX + completionId;
                redisTemplate.delete(redisKey);
                redisTemplate.delete(redisKey + ":processing");
                redisTemplate.delete(redisKey + ":processed");
                return true;
            }
            
            // 标记账单为已补偿状态
            usageRecordService.markAsCompensated(completionId, messageId);
            
            // 清除所有状态
            String redisKey = PROCESSED_KEY_PREFIX + completionId;
            redisTemplate.delete(redisKey);
            redisTemplate.delete(redisKey + ":processing");
            redisTemplate.delete(redisKey + ":processed");
            
            return true;
        } catch (Exception e) {
            logger.error("补偿账单状态失败, businessKey: {}", businessKey, e);
            return false;
        }
    }

    private BigDecimal calculateInputCost(UsageCalculationDTO dto, String timePeriod) {
        try {
            log.info("Calculating input cost for model: {}, timePeriod: {}, tokens: {}/{}",
                    dto.getModelType(), timePeriod,
                    dto.getPromptCacheHitTokens(), dto.getPromptCacheMissTokens());

            // 防止modelType为null，使用默认模型
            String modelType = dto.getModelType();
            if (modelType == null || modelType.isEmpty()) {
                modelType = "default";
                log.warn("Model type is null, using default model for input pricing");
            }

            // 对于输入成本，需要分别处理cache hit和cache miss的情况
            BigDecimal totalCost = BigDecimal.ZERO;
            
            // 防止null值异常，设置默认值为0
            int promptCacheHitTokens = dto.getPromptCacheHitTokens() != null ? dto.getPromptCacheHitTokens() : 0;
            int promptCacheMissTokens = dto.getPromptCacheMissTokens() != null ? dto.getPromptCacheMissTokens() : 0;

            // 处理cache hit的tokens
            if (promptCacheHitTokens > 0) {
                PriceQuery hitQuery = new PriceQuery(
                    modelType,
                    timePeriod,
                    "hit",
                    INPUT_TYPE
                );
                PriceConfig hitConfig = priceCache.getPriceConfig(hitQuery);
                if (hitConfig != null) {
                    totalCost = totalCost.add(
                        calculateTokenCost(promptCacheHitTokens, hitConfig.getPrice())
                    );
                    log.debug("Cache hit cost calculated: tokens={}, price={}, cost={}",
                            promptCacheHitTokens, hitConfig.getPrice(), totalCost);
                } else {
                    log.warn("No price config found for cache hit, using standard price");
                    // 如果找不到cache hit的配置，尝试使用标准配置
                    PriceQuery standardQuery = new PriceQuery(
                        modelType,
                        "standard",
                        null,
                        INPUT_TYPE
                    );
                    PriceConfig standardConfig = priceCache.getPriceConfig(standardQuery);
                    if (standardConfig != null) {
                        totalCost = totalCost.add(
                            calculateTokenCost(promptCacheHitTokens, standardConfig.getPrice())
                        );
                    } else if (!"default".equals(modelType)) {
                        // 尝试使用默认模型的价格配置
                        PriceQuery defaultQuery = new PriceQuery(
                            "default",
                            "standard",
                            null,
                            INPUT_TYPE
                        );
                        PriceConfig defaultConfig = priceCache.getPriceConfig(defaultQuery);
                        if (defaultConfig != null) {
                            totalCost = totalCost.add(
                                calculateTokenCost(promptCacheHitTokens, defaultConfig.getPrice())
                            );
                        } else {
                            // 使用硬编码的兜底价格
                            BigDecimal defaultPrice = new BigDecimal("0.001"); // 每百万token 1美分的兜底价格
                            log.warn("Using hardcoded fallback price for input (hit): {}", defaultPrice);
                            totalCost = totalCost.add(
                                calculateTokenCost(promptCacheHitTokens, defaultPrice)
                            );
                        }
                    }
                }
            }

            // 处理cache miss的tokens
            if (promptCacheMissTokens > 0) {
                PriceQuery missQuery = new PriceQuery(
                    modelType,
                    timePeriod,
                    "miss",
                    INPUT_TYPE
                );
                PriceConfig missConfig = priceCache.getPriceConfig(missQuery);
                if (missConfig != null) {
                    totalCost = totalCost.add(
                        calculateTokenCost(promptCacheMissTokens, missConfig.getPrice())
                    );
                    log.debug("Cache miss cost calculated: tokens={}, price={}, cost={}",
                            promptCacheMissTokens, missConfig.getPrice(), totalCost);
                } else {
                    log.warn("No price config found for cache miss, using standard price");
                    // 如果找不到cache miss的配置，尝试使用标准配置
                    PriceQuery standardQuery = new PriceQuery(
                        modelType,
                        "standard",
                        null,
                        INPUT_TYPE
                    );
                    PriceConfig standardConfig = priceCache.getPriceConfig(standardQuery);
                    if (standardConfig != null) {
                        totalCost = totalCost.add(
                            calculateTokenCost(promptCacheMissTokens, standardConfig.getPrice())
                        );
                    } else if (!"default".equals(modelType)) {
                        // 尝试使用默认模型的价格配置
                        PriceQuery defaultQuery = new PriceQuery(
                            "default",
                            "standard",
                            null,
                            INPUT_TYPE
                        );
                        PriceConfig defaultConfig = priceCache.getPriceConfig(defaultQuery);
                        if (defaultConfig != null) {
                            totalCost = totalCost.add(
                                calculateTokenCost(promptCacheMissTokens, defaultConfig.getPrice())
                            );
                        } else {
                            // 使用硬编码的兜底价格
                            BigDecimal defaultPrice = new BigDecimal("0.0015"); // 每百万token 1.5美分的兜底价格
                            log.warn("Using hardcoded fallback price for input (miss): {}", defaultPrice);
                            totalCost = totalCost.add(
                                calculateTokenCost(promptCacheMissTokens, defaultPrice)
                            );
                        }
                    }
                }
            }

            log.info("Total input cost calculated: {}", totalCost);
            return totalCost;
            
        } catch (Exception e) {
            log.error("Error calculating input cost: model={}, timePeriod={}, error={}",
                    dto.getModelType(), timePeriod, e.getMessage(), e);
            throw new BillingException("Failed to calculate input cost: " + e.getMessage());
        }
    }

    private BigDecimal calculateOutputCost(UsageCalculationDTO dto, String timePeriod) {
        try {
            log.info("Calculating output cost for model: {}, timePeriod: {}, tokens: {}",
                    dto.getModelType(), timePeriod, dto.getCompletionTokens());

            // 防止null值异常，设置默认值为0
            int completionTokens = dto.getCompletionTokens() != null ? dto.getCompletionTokens() : 0;
            
            // 防止modelType为null，使用默认模型
            String modelType = dto.getModelType();
            if (modelType == null || modelType.isEmpty()) {
                modelType = "default";
                log.warn("Model type is null, using default model for pricing");
            }
            
            // 尝试获取特定时段的输出价格配置
            PriceQuery query = new PriceQuery(
                modelType,
                timePeriod,
                null,
                OUTPUT_TYPE
            );
            PriceConfig config = priceCache.getPriceConfig(query);
            
            // 如果找不到当前时段的价格配置（例如discount时段），尝试获取标准时段的价格
            if (config == null && !"standard".equals(timePeriod)) {
                log.warn("No price config found for time period: {}, trying standard time period", timePeriod);
                query = new PriceQuery(
                    modelType,
                    "standard",
                    null,
                    OUTPUT_TYPE
                );
                config = priceCache.getPriceConfig(query);
            }
            
            // 如果仍找不到，尝试使用默认模型的价格配置
            if (config == null && !"default".equals(modelType)) {
                log.warn("No price config found for model: {}, trying default model", modelType);
                query = new PriceQuery(
                    "default",
                    "standard",
                    null,
                    OUTPUT_TYPE
                );
                config = priceCache.getPriceConfig(query);
            }
            
            // 最后的兜底价格
            if (config == null) {
                log.error("No price config found for output: model={}, timePeriod={}",
                        modelType, timePeriod);
                // 使用硬编码的兜底价格，确保系统不会因为找不到价格配置而崩溃
                BigDecimal defaultPrice = new BigDecimal("0.002"); // 每百万token 2美分的兜底价格
                log.warn("Using hardcoded fallback price: {}", defaultPrice);
                return calculateTokenCost(completionTokens, defaultPrice);
            }

            BigDecimal cost = calculateTokenCost(completionTokens, config.getPrice());
            log.info("Output cost calculated: tokens={}, price={}, cost={}",
                    completionTokens, config.getPrice(), cost);
            
            return cost;
        } catch (Exception e) {
            log.error("Error calculating output cost: model={}, timePeriod={}, error={}",
                    dto.getModelType(), timePeriod, e.getMessage(), e);
            throw new BillingException("Failed to calculate output cost: " + e.getMessage());
        }
    }

    private BigDecimal calculateTokenCost(int tokens, BigDecimal pricePerMillion) {
        return BigDecimal.valueOf(tokens)
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP)
                .multiply(pricePerMillion)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private Integer getPriceVersion(UsageCalculationDTO dto, String timePeriod) {
        // 防止modelType为null，使用默认模型
        String modelType = dto.getModelType();
        if (modelType == null || modelType.isEmpty()) {
            modelType = "default";
            log.warn("Model type is null, using default model for price version");
        }
        
        // 首先尝试获取特定时段的价格配置
        PriceQuery query = new PriceQuery(
            modelType,
            timePeriod,
            null,
            OUTPUT_TYPE
        );
        PriceConfig config = priceCache.getPriceConfig(query);
        
        // 如果找不到当前时段的价格配置，尝试获取标准时段的价格
        if (config == null && !"standard".equals(timePeriod)) {
            log.debug("No price config found for time period: {}, trying standard time period for version", timePeriod);
            query = new PriceQuery(
                modelType,
                "standard",
                null,
                OUTPUT_TYPE
            );
            config = priceCache.getPriceConfig(query);
        }
        
        // 如果仍找不到，尝试使用默认模型的价格配置
        if (config == null && !"default".equals(modelType)) {
            log.debug("No price config found for model: {}, trying default model for version", modelType);
            query = new PriceQuery(
                "default",
                "standard",
                null,
                OUTPUT_TYPE
            );
            config = priceCache.getPriceConfig(query);
        }
        
        if (config == null) {
            log.warn("Price config not found for version lookup, using default version 1");
            return 1;
        }
        return config.getVersion();
    }
} 