package com.bgpay.bgai.service;

import io.seata.spring.annotation.GlobalTransactional;
import io.seata.saga.engine.StateMachineEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Seata分布式事务服务示例
 * 演示AT模式和Saga模式的使用
 */
@Service
public class SeataTransactionService {

    @Autowired(required = false)
    private StateMachineEngine stateMachineEngine;

    /**
     * 使用AT模式（两阶段提交）进行分布式事务处理
     * 当任何微服务出现异常时，所有微服务的事务都会回滚
     */
    @GlobalTransactional(name = "bgai-at-tx", rollbackFor = Exception.class)
    @Transactional
    public void executeATTransaction() {
        // 业务逻辑 - 多个微服务之间的事务
        System.out.println("执行分布式事务AT模式...");
        
        // 第一阶段：准备资源并锁定
        // 第二阶段：提交或回滚
        
        // 模拟其他微服务调用
        // 如果任何服务抛出异常，整个事务回滚
    }

    /**
     * 使用Saga模式进行长事务补偿
     * 当事务失败时，会执行补偿操作
     */
    public void executeSagaTransaction() {
        // Saga模式处理
        if (stateMachineEngine != null) {
            String businessKey = "bgai-saga-tx-" + System.currentTimeMillis();
            Map<String, Object> startParams = new HashMap<>();
            startParams.put("businessKey", businessKey);
            // 启动状态机执行，第二个参数为租户ID，这里使用null表示默认租户
            stateMachineEngine.start("bgai-saga-transaction", null, startParams);
        }
    }
} 