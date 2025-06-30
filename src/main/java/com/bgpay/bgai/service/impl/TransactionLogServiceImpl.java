package com.bgpay.bgai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bgpay.bgai.entity.TransactionLog;
import com.bgpay.bgai.mapper.TransactionLogMapper;
import com.bgpay.bgai.service.TransactionLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 分布式事务日志服务实现
 * 使用独立事务记录分布式事务信息，与Seata分离
 */
@Slf4j
@Service
public class TransactionLogServiceImpl implements TransactionLogService {

    @Autowired
    private TransactionLogMapper transactionLogMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long recordTransactionBegin(String xid, String transactionName, String transactionMode,
                                      String requestPath, String sourceIp, String userId) {
        try {
            log.info("记录操作开始 - ID: {}, 名称: {}, 模式: {}, 路径: {}, IP: {}, 用户: {}",
                    xid, transactionName, transactionMode, requestPath, sourceIp, userId);
            Map<String, Object> extraDataMap = new HashMap<>();
            extraDataMap.put("startTime", System.currentTimeMillis());
            extraDataMap.put("result", "active");
            
            String extraData = new ObjectMapper().writeValueAsString(extraDataMap);
            
            TransactionLog transactionLog = new TransactionLog()
                    .setXid(xid)
                    .setTransactionName(transactionName)
                    .setTransactionMode(transactionMode)
                    .setRequestPath(requestPath)
                    .setSourceIp(sourceIp)
                    .setUserId(userId)
                    .setStatus("ACTIVE")
                    .setBranchIds(new ArrayList<>())
                    .setExtraData(extraData)
                    .setStartTime(LocalDateTime.now())
                    .setCreateTime(LocalDateTime.now());
            
            transactionLogMapper.insert(transactionLog);
            return transactionLog.getId();
        } catch (Exception e) {
            log.error("记录操作开始失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateTransactionStatus(String xid, String status, String extraData) {
        log.debug("更新操作状态: ID={}, 状态={}", xid, status);
        
        try {
            TransactionLog txLog = findByXid(xid);
            if (txLog == null) {
                log.warn("未找到操作记录: ID={}", xid);
                return;
            }
            
            // 更新事务状态
            txLog.setStatus(status)
                .setExtraData(extraData)
                .setUpdateTime(LocalDateTime.now());
            
            transactionLogMapper.updateById(txLog);
            log.info("更新操作状态成功: ID={}, 状态={}", xid, status);
        } catch (Exception e) {
            log.error("更新操作状态失败: ID={}", xid, e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTransactionEnd(String xid, String status, String extraData) {
        log.info("记录操作结束: ID={}, 状态={}", xid, status);
        
        try {
            TransactionLog txLog = findByXid(xid);
            if (txLog == null) {
                log.warn("未找到操作记录: ID={}", xid);
                return;
            }
            
            // 更新操作结束状态
            txLog.setStatus(status)
                .setExtraData(extraData)
                .setEndTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
            
            transactionLogMapper.updateById(txLog);
            log.info("记录操作结束成功: ID={}, 状态={}", xid, status);
        } catch (Exception e) {
            log.error("记录操作结束失败: ID={}", e.getMessage(), e);
        }
    }

    @Override
    public TransactionLog findByXid(String xid) {
        return transactionLogMapper.selectOne(
                new LambdaQueryWrapper<TransactionLog>()
                        .eq(TransactionLog::getXid, xid)
                        .last("LIMIT 1")
        );
    }
} 