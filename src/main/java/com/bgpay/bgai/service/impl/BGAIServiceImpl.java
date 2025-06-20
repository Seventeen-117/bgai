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
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String INPUT_TYPE = "input";
    private static final String OUTPUT_TYPE = "output";
    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");
    
    @Autowired
    private RocketMQProducerService rocketMQProducer;
    
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
            usageRecordService.markAsCompensated(completionId);
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
            record.setInputTokens(dto.getPromptCacheHitTokens() + dto.getPromptCacheMissTokens());
            record.setOutputTokens(dto.getCompletionTokens());
            record.setStatus("PENDING");
            record.setPriceVersion(getPriceVersion(dto, timePeriod));

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
    public boolean executeThirdStep(String businessKey, boolean secondStepResult) {
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
            UsageRecord record = usageRecordService.findByCompletionId(completionId);
            if (record == null) {
                log.error("Usage record not found for completionId: {}", completionId);
                return false;
            }

            // 检查记录状态
            if ("COMPLETED".equals(record.getStatus())) {
                log.info("Record already marked as completed: {}", completionId);
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
                usageRecordService.markAsCompleted(completionId);
                
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
            usageRecordService.deleteByCompletionId(completionId);
            
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
    public boolean compensateThirdStep(String businessKey) {
        try {
            logger.info("补偿第三步操作 - 回滚账单状态, businessKey: {}", businessKey);
            String[] parts = businessKey.split(":");
            String completionId = parts[1];
            
            // 检查记录是否存在
            if (!usageRecordService.existsByCompletionId(completionId)) {
                logger.info("记录不存在，无需补偿, completionId: {}", completionId);
                // 清除所有状态
                String redisKey = PROCESSED_KEY_PREFIX + completionId;
                redisTemplate.delete(redisKey);
                redisTemplate.delete(redisKey + ":processing");
                redisTemplate.delete(redisKey + ":processed");
                return true;
            }
            
            // 标记账单为已补偿状态
            usageRecordService.markAsCompensated(completionId);
            
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

            // 对于输入成本，需要分别处理cache hit和cache miss的情况
            BigDecimal totalCost = BigDecimal.ZERO;

            // 处理cache hit的tokens
            if (dto.getPromptCacheHitTokens() > 0) {
                PriceQuery hitQuery = new PriceQuery(
                    dto.getModelType(),
                    timePeriod,
                    "hit",
                    INPUT_TYPE
                );
                PriceConfig hitConfig = priceCache.getPriceConfig(hitQuery);
                if (hitConfig != null) {
                    totalCost = totalCost.add(
                        calculateTokenCost(dto.getPromptCacheHitTokens(), hitConfig.getPrice())
                    );
                    log.debug("Cache hit cost calculated: tokens={}, price={}, cost={}",
                            dto.getPromptCacheHitTokens(), hitConfig.getPrice(), totalCost);
                } else {
                    log.warn("No price config found for cache hit, using standard price");
                    // 如果找不到cache hit的配置，尝试使用标准配置
                    PriceQuery standardQuery = new PriceQuery(
                        dto.getModelType(),
                        timePeriod,
                        null,
                        INPUT_TYPE
                    );
                    PriceConfig standardConfig = priceCache.getPriceConfig(standardQuery);
                    if (standardConfig != null) {
                        totalCost = totalCost.add(
                            calculateTokenCost(dto.getPromptCacheHitTokens(), standardConfig.getPrice())
                        );
                    } else {
                        throw new BillingException("No price config found for input (cache hit)");
                    }
                }
            }

            // 处理cache miss的tokens
            if (dto.getPromptCacheMissTokens() > 0) {
                PriceQuery missQuery = new PriceQuery(
                    dto.getModelType(),
                    timePeriod,
                    "miss",
                    INPUT_TYPE
                );
                PriceConfig missConfig = priceCache.getPriceConfig(missQuery);
                if (missConfig != null) {
                    totalCost = totalCost.add(
                        calculateTokenCost(dto.getPromptCacheMissTokens(), missConfig.getPrice())
                    );
                    log.debug("Cache miss cost calculated: tokens={}, price={}, cost={}",
                            dto.getPromptCacheMissTokens(), missConfig.getPrice(), totalCost);
                } else {
                    log.warn("No price config found for cache miss, using standard price");
                    // 如果找不到cache miss的配置，尝试使用标准配置
                    PriceQuery standardQuery = new PriceQuery(
                        dto.getModelType(),
                        timePeriod,
                        null,
                        INPUT_TYPE
                    );
                    PriceConfig standardConfig = priceCache.getPriceConfig(standardQuery);
                    if (standardConfig != null) {
                        totalCost = totalCost.add(
                            calculateTokenCost(dto.getPromptCacheMissTokens(), standardConfig.getPrice())
                        );
                    } else {
                        throw new BillingException("No price config found for input (cache miss)");
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

            // 首先尝试获取标准输出价格配置
            PriceQuery query = new PriceQuery(
                dto.getModelType(),
                timePeriod,
                null,
                OUTPUT_TYPE
            );
            PriceConfig config = priceCache.getPriceConfig(query);
            
            if (config == null) {
                // 如果找不到特定时段的价格，尝试获取默认价格配置
                query = new PriceQuery(
                    dto.getModelType(),
                    "standard",
                    null,
                    OUTPUT_TYPE
                );
                config = priceCache.getPriceConfig(query);
                
                if (config == null) {
                    log.error("No price config found for output: model={}, timePeriod={}",
                            dto.getModelType(), timePeriod);
                    throw new BillingException("No price config found for output");
                }
                log.warn("Using standard price config for output cost calculation");
            }

            BigDecimal cost = calculateTokenCost(dto.getCompletionTokens(), config.getPrice());
            log.info("Output cost calculated: tokens={}, price={}, cost={}",
                    dto.getCompletionTokens(), config.getPrice(), cost);
            
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
        PriceQuery query = new PriceQuery(
            dto.getModelType(),
            timePeriod,
            null,
            OUTPUT_TYPE
        );
        PriceConfig config = priceCache.getPriceConfig(query);
        if (config == null) {
            log.warn("Price config not found, using default version 1");
            return 1;
        }
        return config.getVersion();
    }
} 