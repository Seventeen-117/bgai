package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.bgpay.bgai.entity.UsageRecord;
import com.bgpay.bgai.service.UsageRecordService;
import com.bgpay.bgai.service.mq.RocketMQProducerService;
import com.bgpay.bgai.service.PriceCacheService;
import com.bgpay.bgai.entity.PriceConfig;
import com.bgpay.bgai.entity.PriceQuery;
import com.bgpay.bgai.exception.BillingException;
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
@Service("bgaiService")
public class BGAIServiceImpl {
    
    private static final Logger logger = LoggerFactory.getLogger(BGAIServiceImpl.class);
    private static final String PROCESSED_KEY_PREFIX = "PROCESSED:";
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    
    @Autowired
    private RocketMQProducerService rocketMQProducer;
    
    @Autowired
    private UsageRecordService usageRecordService;
    
    @Autowired
    private PriceCacheService priceCache;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    /**
     * 执行第一步操作：发送账单消息
     */
    @Transactional
    public boolean executeFirstStep(String businessKey) {
        try {
            logger.info("执行第一步操作 - 发送账单消息, businessKey: {}", businessKey);
            String[] parts = businessKey.split(":");
            String userId = parts[0];
            String completionId = parts[1];
            
            // 检查是否已经处理过
            boolean exists = usageRecordService.existsByCompletionId(completionId);
            logger.info("检查记录是否存在: completionId={}, exists={}", completionId, exists);
            
            if (exists) {
                logger.info("账单消息已处理，跳过发送, completionId: {}", completionId);
                return true;
            }
            
            // 从缓存或其他地方获取 UsageCalculationDTO
            UsageCalculationDTO dto = usageRecordService.getCalculationDTO(completionId);
            if (dto == null) {
                logger.error("未找到计费数据, completionId: {}, userId: {}", completionId, userId);
                throw new BillingException("计费数据不存在");
            }
            
            logger.info("获取到计费数据: completionId={}, modelType={}, tokens={}/{}", 
                completionId, dto.getModelType(), dto.getPromptTokens(), dto.getCompletionTokens());
            
            // 检查Redis状态并清理可能存在的旧状态
            String redisKey = PROCESSED_KEY_PREFIX + completionId;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                logger.warn("发现旧的处理状态，执行清理: {}", redisKey);
                redisTemplate.delete(redisKey);
                redisTemplate.delete(redisKey + ":processing");
                redisTemplate.delete(redisKey + ":processed");
            }
            
            // 发送账单消息
            rocketMQProducer.sendBillingMessage(dto, userId);
            
            // 设置处理中状态
            redisTemplate.opsForValue().set(redisKey + ":processing", "1", 30, TimeUnit.MINUTES);
            logger.info("成功设置处理中状态: {}", redisKey + ":processing");
            
            return true;
        } catch (Exception e) {
            logger.error("发送账单消息失败, businessKey: {}, error: {}", businessKey, e.getMessage(), e);
            throw new BillingException("发送账单消息失败: " + e.getMessage());
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
     * 执行第二步操作：处理账单消息
     */
    @Transactional
    public boolean executeSecondStep(String businessKey, Object firstResult) {
        try {
            logger.info("执行第二步操作 - 处理账单消息, businessKey: {}, firstResult: {}", businessKey, firstResult);
            String[] parts = businessKey.split(":");
            String userId = parts[0];
            String completionId = parts[1];
            
            // 检查消息是否已经处理
            boolean recordExists = usageRecordService.existsByCompletionId(completionId);
            logger.info("检查记录是否存在: completionId={}, exists={}", completionId, recordExists);
            
            if (recordExists) {
                logger.info("账单消息已处理，跳过处理, completionId: {}", completionId);
                // 更新处理状态
                String redisKey = PROCESSED_KEY_PREFIX + completionId;
                redisTemplate.opsForValue().set(redisKey + ":processed", "1", 24, TimeUnit.HOURS);
                return true;
            }
            
            // 检查是否在处理中
            String redisKey = PROCESSED_KEY_PREFIX + completionId;
            boolean isProcessing = Boolean.TRUE.equals(redisTemplate.hasKey(redisKey + ":processing"));
            logger.info("检查处理中状态: completionId={}, isProcessing={}", completionId, isProcessing);
            
            if (!isProcessing) {
                logger.error("消息未在处理中状态, completionId: {}", completionId);
                throw new BillingException("消息未在处理中状态");
            }
            
            // 获取并处理账单数据
            UsageCalculationDTO dto = usageRecordService.getCalculationDTO(completionId);
            if (dto == null) {
                logger.error("未找到计费数据, completionId: {}, userId: {}", completionId, userId);
                throw new BillingException("计费数据不存在");
            }
            
            logger.info("获取到计费数据: completionId={}, modelType={}, tokens={}/{}", 
                completionId, dto.getModelType(), dto.getPromptTokens(), dto.getCompletionTokens());
            
            try {
//                 计算费用并保存使用记录
                UsageRecord record = convertToUsageRecord(dto, userId);
                usageRecordService.insertUsageRecord(record);
                logger.info("成功插入使用记录: completionId={}, userId={}", completionId, userId);
                
                // 更新处理状态
                redisTemplate.opsForValue().set(redisKey + ":processed", "1", 24, TimeUnit.HOURS);
                logger.info("成功设置处理完成状态: {}", redisKey + ":processed");
                
                return true;
            } catch (DuplicateKeyException e) {
                logger.warn("记录已存在，可能是并发处理, completionId: {}, error: {}", completionId, e.getMessage());
                redisTemplate.opsForValue().set(redisKey + ":processed", "1", 24, TimeUnit.HOURS);
                return true;
            }
        } catch (Exception e) {
            logger.error("处理账单消息失败, businessKey: {}, error: {}", businessKey, e.getMessage(), e);
            throw new BillingException("处理账单消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行第三步操作：更新账单状态
     */
    @Transactional
    public boolean executeThirdStep(String businessKey, Object secondResult) {
        try {
            logger.info("执行第三步操作 - 更新账单状态, businessKey: {}, secondResult: {}", businessKey, secondResult);
            String[] parts = businessKey.split(":");
            String completionId = parts[1];
            
            // 检查是否已处理完成
            String redisKey = PROCESSED_KEY_PREFIX + completionId;
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(redisKey + ":processed"))) {
                logger.error("账单未处理完成, completionId: {}", completionId);
                return false;
            }
            
            // 检查记录是否存在
            if (!usageRecordService.existsByCompletionId(completionId)) {
                logger.error("未找到账单记录, completionId: {}", completionId);
                return false;
            }
            
            try {
                // 标记账单为已完成状态
                usageRecordService.markAsCompleted(completionId);
                
                // 设置最终状态
                redisTemplate.opsForValue().set(redisKey, "1", 24, TimeUnit.HOURS);
                redisTemplate.delete(redisKey + ":processing");
                redisTemplate.delete(redisKey + ":processed");
                
                logger.info("账单处理完成, completionId: {}", completionId);
                return true;
            } catch (Exception e) {
                logger.error("更新账单状态失败, completionId: {}", completionId, e);
                return false;
            }
        } catch (Exception e) {
            logger.error("更新账单状态失败, businessKey: {}", businessKey, e);
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

    /**
     * 将UsageCalculationDTO转换为UsageRecord
     */
    private UsageRecord convertToUsageRecord(UsageCalculationDTO dto, String userId) {
        UsageRecord record = new UsageRecord();
        record.setModelType(dto.getModelType());
        record.setChatCompletionId(dto.getChatCompletionId());
        record.setUserId(userId);
        record.setCalculatedAt(LocalDateTime.now());
        record.setStatus("PENDING");
        record.setCreatedAt(LocalDateTime.now());

        // 计算费用
        ZonedDateTime beijingTime = dto.getCreatedAt()
                .atZone(ZoneId.systemDefault())
                .withZoneSameInstant(BEIJING_ZONE);
        String timePeriod = determineTimePeriod(beijingTime);
        
        // 计算输入成本
        BigDecimal inputCost = calculateInputCost(dto, timePeriod);
        record.setInputCost(inputCost);
        
        // 计算输出成本
        BigDecimal outputCost = calculateOutputCost(dto, timePeriod);
        record.setOutputCost(outputCost);
        
        // 设置价格版本
        record.setPriceVersion(getPriceVersion(dto, timePeriod));
        
        return record;
    }

    private String determineTimePeriod(ZonedDateTime beijingTime) {
        // 实现时间段判断逻辑
        return "standard"; // 根据实际需求实现
    }

    private BigDecimal calculateInputCost(UsageCalculationDTO dto, String timePeriod) {
        try {
            PriceQuery query = new PriceQuery(dto.getModelType(), timePeriod, null, INPUT_TYPE);
            PriceConfig config = priceCache.getPriceConfig(query);
            
            if (config == null) {
                logger.warn("No price configuration found for input cost calculation. Using default pricing. Query: {}", query);
                // Default pricing: 0.002 per 1K tokens for input
                return calculateCost(dto.getPromptTokens(), new BigDecimal("2.0"));
            }
            
            return calculateCost(dto.getPromptTokens(), config.getPrice());
        } catch (Exception e) {
            logger.error("Error calculating input cost for model: {}, timePeriod: {}", dto.getModelType(), timePeriod, e);
            // Default pricing as fallback
            return calculateCost(dto.getPromptTokens(), new BigDecimal("2.0"));
        }
    }

    private BigDecimal calculateOutputCost(UsageCalculationDTO dto, String timePeriod) {
        try {
            PriceQuery query = new PriceQuery(dto.getModelType(), timePeriod, null, OUTPUT_TYPE);
            PriceConfig config = priceCache.getPriceConfig(query);
            
            if (config == null) {
                logger.warn("No price configuration found for output cost calculation. Using default pricing. Query: {}", query);
                // Default pricing: 0.004 per 1K tokens for output
                return calculateCost(dto.getCompletionTokens(), new BigDecimal("4.0"));
            }
            
            return calculateCost(dto.getCompletionTokens(), config.getPrice());
        } catch (Exception e) {
            logger.error("Error calculating output cost for model: {}, timePeriod: {}", dto.getModelType(), timePeriod, e);
            // Default pricing as fallback
            return calculateCost(dto.getCompletionTokens(), new BigDecimal("4.0"));
        }
    }

    private BigDecimal calculateCost(int tokens, BigDecimal pricePerMillion) {
        return BigDecimal.valueOf(tokens)
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP)
                .multiply(pricePerMillion)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private Integer getPriceVersion(UsageCalculationDTO dto, String timePeriod) {
        try {
            PriceQuery query = new PriceQuery(dto.getModelType(), timePeriod, null, OUTPUT_TYPE);
            PriceConfig config = priceCache.getPriceConfig(query);
            
            if (config == null) {
                logger.warn("No price configuration found for version lookup. Using default version 1. Query: {}", query);
                return 1;
            }
            
            return config.getVersion();
        } catch (Exception e) {
            logger.error("Error getting price version for model: {}, timePeriod: {}", dto.getModelType(), timePeriod, e);
            return 1;
        }
    }
} 