package com.bgpay.bgai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bgpay.bgai.entity.TransactionLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分布式事务日志Mapper接口
 */
@Mapper
public interface TransactionLogMapper extends BaseMapper<TransactionLog> {
} 