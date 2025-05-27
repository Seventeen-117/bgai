package com.bgpay.bgai.service;

import com.bgpay.bgai.entity.TransactionLog;

/**
 * 事务日志服务接口
 */
public interface TransactionLogService {

    /**
     * 记录事务开始
     * @param xid 事务ID
     * @param transactionName 事务名称
     * @param transactionMode 事务模式
     * @param requestPath 请求路径
     * @param sourceIp 来源IP
     * @param userId 用户ID
     * @return 事务日志ID
     */
    Long recordTransactionBegin(String xid, String transactionName, String transactionMode, 
                               String requestPath, String sourceIp, String userId);

    /**
     * 更新事务状态
     * @param xid 事务ID
     * @param status 事务状态
     * @param extraData 额外数据
     */
    void updateTransactionStatus(String xid, String status, String extraData);

    /**
     * 记录事务结束
     * @param xid 事务ID
     * @param status 事务状态
     * @param extraData 额外数据
     */
    void recordTransactionEnd(String xid, String status, String extraData);

    /**
     * 根据XID查询事务日志
     * @param xid 事务ID
     * @return 事务日志
     */
    TransactionLog findByXid(String xid);
} 