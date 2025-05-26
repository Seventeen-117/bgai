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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 分布式事务日志服务实现
 * 使用独立事务记录分布式事务信息
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
        log.info("记录分布式事务开始: XID={}, 业务={}", xid, transactionName);
        
        try {
            // 从XID中提取branchId（如果有的话）
            String branchId = "";
            if (xid != null && xid.split(":").length > 2) {
                branchId = xid.split(":")[2];
            }
            
            // 构建初始extraData
            String extraData = String.format(
                "{\"startTime\":%d,\"result\":\"active\",\"branchId\":\"%s\"}",
                System.currentTimeMillis(),
                branchId
            );
            
            // 初始化branchIds列表
            List<String> branchIds = new ArrayList<>();
            if (!branchId.isEmpty()) {
                branchIds.add(branchId);
            }
            
            TransactionLog transactionLog = new TransactionLog()
                    .setXid(xid)
                    .setTransactionName(transactionName)
                    .setTransactionMode(transactionMode)
                    .setRequestPath(requestPath)
                    .setSourceIp(sourceIp)
                    .setUserId(userId)
                    .setStatus("ACTIVE")
                    .setBranchId(branchId)  // 设置当前branchId
                    .setBranchIds(branchIds)  // 设置branchIds列表
                    .setExtraData(extraData) // 设置extraData
                    .setStartTime(LocalDateTime.now())
                    .setCreateTime(LocalDateTime.now());
            
            transactionLogMapper.insert(transactionLog);
            return transactionLog.getId();
        } catch (Exception e) {
            log.error("记录事务开始失败: XID={}", xid, e);
            // 不要抛出异常，避免影响主业务流程
            return null;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean updateTransactionStatus(String xid, String status, String branchId) {
        log.debug("更新分布式事务状态: XID={}, 状态={}, 分支={}", xid, status, branchId);
        
        try {
            TransactionLog txLog = findByXid(xid);
            if (txLog == null) {
                log.warn("未找到事务记录: XID={}, 尝试创建新记录", xid);
                // 构建extraData
                String extraData = String.format(
                    "{\"updateTime\":%d,\"result\":\"active\",\"branchId\":\"%s\"}",
                    System.currentTimeMillis(),
                    branchId
                );
                
                // 初始化branchIds列表
                List<String> branchIds = new ArrayList<>();
                if (branchId != null && !branchId.isEmpty()) {
                    branchIds.add(branchId);
                }
                
                // 尝试创建一个新记录，以便记录分支ID
                txLog = new TransactionLog()
                    .setXid(xid)
                    .setTransactionName("auto-created")
                    .setTransactionMode("AT")
                    .setStatus(status)
                    .setBranchId(branchId)
                    .setBranchIds(branchIds)  // 设置branchIds列表
                    .setExtraData(extraData)
                    .setStartTime(LocalDateTime.now())
                    .setCreateTime(LocalDateTime.now())
                    .setUpdateTime(LocalDateTime.now());
                
                transactionLogMapper.insert(txLog);
                log.info("为XID={}创建了新的事务记录，分支ID={}", xid, branchId);
                return true;
            }
            
            // 更新branchIds列表
            List<String> branchIds = txLog.getBranchIds();
            if (branchIds == null) {
                branchIds = new ArrayList<>();
            }
            if (branchId != null && !branchId.isEmpty() && !branchIds.contains(branchId)) {
                branchIds.add(branchId);
            }
            
            // 更新extraData
            String extraData = String.format(
                "{\"updateTime\":%d,\"result\":\"%s\",\"branchId\":\"%s\",\"previousBranchId\":\"%s\",\"totalBranches\":%d}",
                System.currentTimeMillis(),
                status.toLowerCase(),
                branchId,
                txLog.getBranchId(),
                branchIds.size()
            );
            
            txLog.setStatus(status);
            txLog.setBranchId(branchId);
            txLog.setBranchIds(branchIds);  // 更新branchIds列表
            txLog.setExtraData(extraData);
            txLog.setUpdateTime(LocalDateTime.now());
            
            return transactionLogMapper.updateById(txLog) > 0;
        } catch (Exception e) {
            log.error("更新事务状态失败: XID={}", xid, e);
            return false;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordTransactionEnd(String xid, String status, String extraData) {
        log.info("记录分布式事务结束: XID={}, 状态={}", xid, status);
        
        try {
            TransactionLog txLog = findByXid(xid);
            if (txLog == null) {
                log.warn("未找到事务记录: XID={}, 尝试创建新记录", xid);
                
                // 从XID中提取branchId（如果有的话）
                String branchId = "";
                if (xid != null && xid.split(":").length > 2) {
                    branchId = xid.split(":")[2];
                }
                
                // 初始化branchIds列表
                List<String> branchIds = new ArrayList<>();
                if (!branchId.isEmpty()) {
                    branchIds.add(branchId);
                }
                
                // 尝试创建一个新记录
                txLog = new TransactionLog()
                    .setXid(xid)
                    .setTransactionName("auto-created-end")
                    .setTransactionMode("AT")
                    .setStatus(status)
                    .setBranchId(branchId)
                    .setBranchIds(branchIds)  // 设置branchIds列表
                    .setExtraData(extraData)
                    .setStartTime(LocalDateTime.now())
                    .setEndTime(LocalDateTime.now())
                    .setCreateTime(LocalDateTime.now())
                    .setUpdateTime(LocalDateTime.now());
                
                transactionLogMapper.insert(txLog);
                log.info("为XID={}创建了结束事务记录", xid);
                return true;
            }
            
            txLog.setStatus(status);
            txLog.setExtraData(extraData);
            txLog.setEndTime(LocalDateTime.now());
            txLog.setUpdateTime(LocalDateTime.now());
            
            return transactionLogMapper.updateById(txLog) > 0;
        } catch (Exception e) {
            log.error("记录事务结束失败: XID={}", xid, e);
            return false;
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