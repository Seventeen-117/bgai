package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.bgpay.bgai.entity.UsageRecord;
import com.bgpay.bgai.service.UsageRecordService;
import com.bgpay.bgai.service.mq.RocketMQProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BGAI服务实现，用于Saga状态机
 * 包含正向操作和补偿操作
 */
@Service("bgaiService")
public class BGAIServiceImpl {
    
    private static final Logger logger = LoggerFactory.getLogger(BGAIServiceImpl.class);
    
    @Autowired
    private RocketMQProducerService rocketMQProducer;
    
    @Autowired
    private UsageRecordService usageRecordService;
    
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
            if (usageRecordService.existsByCompletionId(completionId)) {
                logger.info("账单消息已处理，跳过发送, completionId: {}", completionId);
                return true;
            }
            
            // 从缓存或其他地方获取 UsageCalculationDTO
            UsageCalculationDTO dto = usageRecordService.getCalculationDTO(completionId);
            if (dto == null) {
                logger.error("未找到计费数据, completionId: {}", completionId);
                return false;
            }
            
            // 发送账单消息
            rocketMQProducer.sendBillingMessage(dto, userId);
            return true;
        } catch (Exception e) {
            logger.error("发送账单消息失败, businessKey: {}", businessKey, e);
            return false;
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
            String completionId = parts[1];
            
            // 检查消息是否已经处理
            if (usageRecordService.existsByCompletionId(completionId)) {
                logger.info("账单消息已处理，跳过处理, completionId: {}", completionId);
                return true;
            }
            
            // 获取并处理账单数据
            UsageCalculationDTO dto = usageRecordService.getCalculationDTO(completionId);
            if (dto == null) {
                logger.error("未找到计费数据, completionId: {}", completionId);
                return false;
            }
            
            // 保存使用记录
            UsageRecord record = convertToUsageRecord(dto, parts[0]);
            usageRecordService.insertUsageRecord(record);
            return true;
        } catch (Exception e) {
            logger.error("处理账单消息失败, businessKey: {}", businessKey, e);
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
            
            // 删除使用记录
            usageRecordService.deleteByCompletionId(completionId);
            return true;
        } catch (Exception e) {
            logger.error("补偿账单处理失败, businessKey: {}", businessKey, e);
            return false;
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
            
            // 标记账单为已完成状态
            usageRecordService.markAsCompleted(completionId);
            return true;
        } catch (Exception e) {
            logger.error("更新账单状态失败, businessKey: {}", businessKey, e);
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
            
            // 标记账单为已补偿状态
            usageRecordService.markAsCompensated(completionId);
            return true;
        } catch (Exception e) {
            logger.error("补偿账单状态失败, businessKey: {}", businessKey, e);
            return false;
        }
    }
    
    private UsageRecord convertToUsageRecord(UsageCalculationDTO dto, String userId) {
        UsageRecord record = new UsageRecord();
        record.setUserId(userId);
        record.setChatCompletionId(dto.getChatCompletionId());
        record.setModelType(dto.getModelType());
        record.setInputCost(dto.getInputCost());
        record.setOutputCost(dto.getOutputCost());
        record.setCalculatedAt(dto.getCreatedAt());
        return record;
    }
} 