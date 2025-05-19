package com.bgpay.bgai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 分布式事务日志实体
 * 用于记录Seata分布式事务的执行情况
 */
@Data
@Accessors(chain = true)
@TableName("transaction_log")
public class TransactionLog {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 全局事务ID (XID)
     */
    private String xid;

    /**
     * 事务分支ID
     */
    private String branchId;

    /**
     * 事务名称/业务标识
     */
    private String transactionName;

    /**
     * 事务模式: AT, SAGA, XA, TCC
     */
    private String transactionMode;

    /**
     * 事务状态: COMMITTED, ROLLBACKED, ACTIVE
     */
    private String status;

    /**
     * 业务请求路径
     */
    private String requestPath;

    /**
     * 请求来源IP
     */
    private String sourceIp;

    /**
     * 事务开始时间
     */
    private LocalDateTime startTime;

    /**
     * 事务结束时间
     */
    private LocalDateTime endTime;

    /**
     * 操作用户ID
     */
    private String userId;

    /**
     * 额外信息 (JSON格式)
     */
    private String extraData;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
} 