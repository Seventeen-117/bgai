package com.bgpay.bgai.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 记录分布式事务的状态信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionStatus implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 交易对应的chatCompletionId
     */
    private String chatCompletionId;
    
    /**
     * 事务所处的阶段
     */
    private TransactionPhase phase;
    
    /**
     * 事务创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 创建事务状态，只设置ID和阶段
     */
    public TransactionStatus(String chatCompletionId, TransactionPhase phase) {
        this.chatCompletionId = chatCompletionId;
        this.phase = phase;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }
    
    /**
     * 更新事务状态
     */
    public void updatePhase(TransactionPhase phase) {
        this.phase = phase;
        this.updatedAt = LocalDateTime.now();
    }
} 