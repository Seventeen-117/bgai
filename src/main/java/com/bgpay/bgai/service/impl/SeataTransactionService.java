package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.service.TransactionLogService;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import io.seata.saga.engine.StateMachineEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Seata分布式事务服务示例
 * 演示AT模式和Saga模式的使用
 */
@Service
@Slf4j
public class SeataTransactionService {

    @Autowired(required = false)
    private StateMachineEngine stateMachineEngine;
    
    @Autowired
    private TransactionLogService transactionLogService;
    
    @Autowired
    private BGAIServiceImpl bgaiService;
    
    @Autowired(required = false)
    @Qualifier("masterJdbcTemplate")
    private JdbcTemplate masterJdbcTemplate;
    
    @Autowired(required = false)
    @Qualifier("slaveJdbcTemplate")
    private JdbcTemplate slaveJdbcTemplate;

    /**
     * 使用AT模式（两阶段提交）进行分布式事务处理
     * 当任何微服务出现异常时，所有微服务的事务都会回滚
     * 
     * @param userId 用户ID
     * @param operationType 操作类型
     * @param requestPath 请求路径
     * @param sourceIp 来源IP
     * @return 操作结果
     */
    @GlobalTransactional(name = "bgai-at-tx", rollbackFor = Exception.class)
    @Transactional
    public boolean executeATTransaction(String userId, String operationType, String requestPath, String sourceIp) {
        // 获取当前Seata全局事务ID
        String xid = RootContext.getXID();
        if (xid == null) {
            log.warn("无法获取Seata全局事务ID，创建手动事务ID");
            xid = "manual-" + UUID.randomUUID().toString();
            RootContext.bind(xid);
        }
        
        log.info("开始AT模式分布式事务: XID={}, 用户={}, 操作={}", xid, userId, operationType);
        
        try {
            // 记录事务开始
            String transactionName = "SeataTransactionService.executeATTransaction";
            Long logId = transactionLogService.recordTransactionBegin(
                    xid, transactionName, "AT", requestPath, sourceIp, userId);
            
            // 第一阶段：准备资源并锁定
            log.info("AT事务第一阶段: 准备并锁定资源");
            if (masterJdbcTemplate != null) {
                // 示例：对主数据库执行写操作
                masterJdbcTemplate.update(
                        "INSERT INTO operation_log (user_id, operation_type, created_at) VALUES (?, ?, ?)",
                        userId, operationType, LocalDateTime.now());
            } else {
                // 模拟操作
                log.info("模拟主数据库写操作: 用户={}, 操作类型={}", userId, operationType);
            }
            
            // 执行业务服务操作
            boolean firstStepResult = bgaiService.executeFirstStep("AT-" + xid);
            if (!firstStepResult) {
                throw new RuntimeException("第一步操作失败");
            }
            
            // 第二阶段：查询并处理数据
            if (slaveJdbcTemplate != null) {
                // 示例：从从数据库读取数据
                int count = slaveJdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM operation_log WHERE user_id = ?", 
                        Integer.class, userId);
                log.info("从数据库查询结果: 用户操作次数={}", count);
            } else {
                // 模拟操作
                log.info("模拟从数据库读操作");
            }
            
            // 执行第二阶段业务逻辑
            boolean secondStepResult = bgaiService.executeSecondStep("AT-" + xid, true);
            if (!secondStepResult) {
                throw new RuntimeException("第二步操作失败");
            }
            
            // 更新事务状态
            String branchId = "branch-" + UUID.randomUUID().toString().substring(0, 8);
            transactionLogService.updateTransactionStatus(xid, "BRANCH_REGISTERED", branchId);
            
            // 模拟第三步操作，此操作可能因为业务条件失败
            boolean thirdStepResult = bgaiService.executeThirdStep("AT-" + xid, secondStepResult);
            if (!thirdStepResult) {
                log.warn("第三步操作失败，整个事务即将回滚");
                throw new RuntimeException("第三步操作失败");
            }
            
            // 记录成功完成
            String extraData = String.format(
                "{\"operation\":\"%s\",\"result\":\"success\",\"branchId\":\"%s\"}", 
                operationType, branchId);
            transactionLogService.recordTransactionEnd(xid, "COMMITTED", extraData);
            
            log.info("AT模式分布式事务成功完成: XID={}", xid);
            return true;
        } catch (Exception e) {
            log.error("AT模式分布式事务执行失败: XID={}, 错误={}", xid, e.getMessage(), e);
            
            // 记录事务失败，不需要手动回滚，@GlobalTransactional 会处理回滚
            String extraData = String.format(
                "{\"operation\":\"%s\",\"result\":\"failure\",\"error\":\"%s\"}", 
                operationType, e.getMessage().replace("\"", "\\\""));
            transactionLogService.recordTransactionEnd(xid, "ROLLBACKED", extraData);
            
            throw e; // 重新抛出异常，触发全局事务回滚
        } finally {
            RootContext.unbind();
        }
    }

    /**
     * 使用Saga模式进行长事务补偿
     * 当事务失败时，会执行补偿操作
     * 
     * @param userId 用户ID
     * @param operationType 操作类型
     * @param requestPath 请求路径
     * @param sourceIp 来源IP
     * @return 操作结果
     */
    public boolean executeSagaTransaction(String userId, String operationType, String requestPath, String sourceIp) {
        if (stateMachineEngine == null) {
            log.error("Saga状态机引擎未初始化，无法执行Saga事务");
            return false;
        }
        
        String businessKey = "bgai-saga-tx-" + UUID.randomUUID().toString();
        log.info("开始Saga模式分布式事务: businessKey={}, 用户={}, 操作={}", businessKey, userId, operationType);
        
        try {
            // 记录事务开始
            String xid = "saga-" + businessKey;
            String transactionName = "SeataTransactionService.executeSagaTransaction";
            Long logId = transactionLogService.recordTransactionBegin(
                    xid, transactionName, "SAGA", requestPath, sourceIp, userId);
            
            // 准备状态机参数
            Map<String, Object> startParams = new HashMap<>();
            startParams.put("businessKey", businessKey);
            startParams.put("userId", userId);
            startParams.put("operationType", operationType);
            startParams.put("startTime", System.currentTimeMillis());
            
            // 启动状态机执行，第二个参数为租户ID，这里使用null表示默认租户
            try {
                log.info("启动Saga状态机: {}", "bgai-saga-transaction");
                stateMachineEngine.start("bgai-saga-transaction", null, startParams);
                
                // 记录事务成功完成
                String extraData = String.format(
                    "{\"businessKey\":\"%s\",\"operation\":\"%s\",\"result\":\"success\"}", 
                    businessKey, operationType);
                transactionLogService.recordTransactionEnd(xid, "COMMITTED", extraData);
                
                log.info("Saga模式分布式事务成功完成: businessKey={}", businessKey);
                return true;
            } catch (Exception e) {
                // 状态机会自动处理补偿，这里只需记录错误
                log.error("Saga状态机执行失败: businessKey={}, 错误={}", businessKey, e.getMessage(), e);
                
                // 记录事务失败
                String extraData = String.format(
                    "{\"businessKey\":\"%s\",\"operation\":\"%s\",\"result\":\"failure\",\"error\":\"%s\"}", 
                    businessKey, operationType, e.getMessage().replace("\"", "\\\""));
                transactionLogService.recordTransactionEnd(xid, "ROLLBACKED", extraData);
                
                return false;
            }
        } catch (Exception e) {
            log.error("Saga事务准备阶段失败: businessKey={}, 错误={}", businessKey, e.getMessage(), e);
            return false;
        }
    }
} 