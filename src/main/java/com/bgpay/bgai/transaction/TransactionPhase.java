package com.bgpay.bgai.transaction;

/**
 * 表示分布式事务的不同阶段
 */
public enum TransactionPhase {
    /**
     * 事务已准备，第一阶段完成
     */
    PREPARED,
    
    /**
     * 事务已提交，第二阶段完成
     */
    COMMITTED,
    
    /**
     * 事务已回滚
     */
    ROLLEDBACK,
    
    /**
     * 事务发生错误后，已执行补偿操作
     */
    COMPENSATED
} 