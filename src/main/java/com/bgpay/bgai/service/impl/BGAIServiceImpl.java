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
                // 处理用户使用信息
                boolean success = usageInfoService.processUsageInfo(dto, userId);
                if (!success) {
                    logger.error("处理用户使用信息失败, completionId: {}", completionId);
                    throw new BillingException("处理用户使用信息失败");
                }
                
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
     * 执行第三步操作：更新账单状态，完成最终处理
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
} 