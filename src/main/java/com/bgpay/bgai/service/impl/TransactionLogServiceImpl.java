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

import java.time.LocalDateTime;

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
            TransactionLog transactionLog = new TransactionLog()
                    .setXid(xid)
                    .setTransactionName(transactionName)
                    .setTransactionMode(transactionMode)
                    .setRequestPath(requestPath)
                    .setSourceIp(sourceIp)
                    .setUserId(userId)
                    .setStatus("ACTIVE")
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
                log.warn("未找到事务记录: XID={}", xid);
                return false;
            }
            
            txLog.setStatus(status);
            txLog.setBranchId(branchId);
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
                log.warn("未找到事务记录: XID={}", xid);
                return false;
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