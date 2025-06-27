package com.bgpay.bgai.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bgpay.bgai.entity.UsageRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author zly
 * @since 2025-03-09 21:17:29
 */
@Mapper
public interface UsageRecordMapper extends BaseMapper<UsageRecord> {
    default UsageRecord findByCompletionId(String completionId) {
        LambdaQueryWrapper<UsageRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UsageRecord::getChatCompletionId, completionId);
        return this.selectOne(queryWrapper);
    }

    default List<UsageRecord> findByModel(String modelType) {
        LambdaQueryWrapper<UsageRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UsageRecord::getModelType, modelType);
        return this.selectList(queryWrapper);
    }

    /**
     * 查询用户的用量记录
     */
    default List<UsageRecord> findUserUsageRecords(String userId, 
                                          String modelType,
                                          LocalDateTime startDateTime,
                                          LocalDateTime endDateTime) {
        LambdaQueryWrapper<UsageRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UsageRecord::getUserId, userId);
        
        if (modelType != null) {
            queryWrapper.eq(UsageRecord::getModelType, modelType);
        }
        
        if (startDateTime != null) {
            queryWrapper.ge(UsageRecord::getCreatedAt, startDateTime);
        }
        
        if (endDateTime != null) {
            queryWrapper.le(UsageRecord::getCreatedAt, endDateTime);
        }
        
        queryWrapper.orderByDesc(UsageRecord::getCreatedAt);
        
        return this.selectList(queryWrapper);
    }

    /**
     * 获取用户总消费
     */
    default BigDecimal getUserTotalCost(String userId,
                               LocalDateTime startDateTime,
                               LocalDateTime endDateTime) {
        // 这里需要用原生SQL聚合函数，所以使用selectMaps
        LambdaQueryWrapper<UsageRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UsageRecord::getUserId, userId)
                .select(UsageRecord::getInputCost, UsageRecord::getOutputCost);
        
        if (startDateTime != null) {
            queryWrapper.ge(UsageRecord::getCreatedAt, startDateTime);
        }
        
        if (endDateTime != null) {
            queryWrapper.le(UsageRecord::getCreatedAt, endDateTime);
        }
        
        List<Map<String, Object>> maps = this.selectMaps(queryWrapper);
        
        BigDecimal totalCost = BigDecimal.ZERO;
        for (Map<String, Object> map : maps) {
            BigDecimal inputCost = (BigDecimal) map.get("input_cost");
            BigDecimal outputCost = (BigDecimal) map.get("output_cost");
            
            if (inputCost != null) {
                totalCost = totalCost.add(inputCost);
            }
            
            if (outputCost != null) {
                totalCost = totalCost.add(outputCost);
            }
        }
        
        return totalCost;
    }

    /**
     * 获取用户总输入token数
     */
    default Integer getUserTotalInputTokens(String userId,
                                   LocalDateTime startDateTime,
                                   LocalDateTime endDateTime) {
        LambdaQueryWrapper<UsageRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UsageRecord::getUserId, userId)
                .select(UsageRecord::getInputTokens);
        
        if (startDateTime != null) {
            queryWrapper.ge(UsageRecord::getCreatedAt, startDateTime);
        }
        
        if (endDateTime != null) {
            queryWrapper.le(UsageRecord::getCreatedAt, endDateTime);
        }
        
        List<UsageRecord> records = this.selectList(queryWrapper);
        
        return records.stream()
                .mapToInt(UsageRecord::getInputTokens)
                .sum();
    }

    /**
     * 获取用户总输出token数
     */
    default Integer getUserTotalOutputTokens(String userId,
                                    LocalDateTime startDateTime,
                                    LocalDateTime endDateTime) {
        LambdaQueryWrapper<UsageRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UsageRecord::getUserId, userId)
                .select(UsageRecord::getOutputTokens);
        
        if (startDateTime != null) {
            queryWrapper.ge(UsageRecord::getCreatedAt, startDateTime);
        }
        
        if (endDateTime != null) {
            queryWrapper.le(UsageRecord::getCreatedAt, endDateTime);
        }
        
        List<UsageRecord> records = this.selectList(queryWrapper);
        
        return records.stream()
                .mapToInt(UsageRecord::getOutputTokens)
                .sum();
    }

    /**
     * 获取用户总请求次数
     */
    default Long getUserTotalRequests(String userId,
                             LocalDateTime startDateTime,
                             LocalDateTime endDateTime) {
        LambdaQueryWrapper<UsageRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UsageRecord::getUserId, userId);
        
        if (startDateTime != null) {
            queryWrapper.ge(UsageRecord::getCreatedAt, startDateTime);
        }
        
        if (endDateTime != null) {
            queryWrapper.le(UsageRecord::getCreatedAt, endDateTime);
        }
        
        return this.selectCount(queryWrapper);
    }

    /**
     * 获取用户最近一次使用时间
     */
    default LocalDateTime getUserLastUsageTime(String userId) {
        LambdaQueryWrapper<UsageRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UsageRecord::getUserId, userId)
                .orderByDesc(UsageRecord::getCreatedAt)
                .last("LIMIT 1")
                .select(UsageRecord::getCreatedAt);
        
        UsageRecord record = this.selectOne(queryWrapper);
        return record != null ? record.getCreatedAt() : null;
    }

    /**
     * 获取用户按模型分组的用量统计 - 这个需要自定义SQL或使用XML配置
     * 由于涉及分组聚合，这里保留原始的SQL查询方式
     */
    @Select("<script>" +
            "SELECT model_type, " +
            "COUNT(*) as request_count, " +
            "SUM(input_tokens) as total_input_tokens, " +
            "SUM(output_tokens) as total_output_tokens, " +
            "SUM(input_cost + output_cost) as total_cost " +
            "FROM usage_record WHERE user_id = #{userId} " +
            "<if test='startDateTime != null'> AND created_at &gt;= #{startDateTime} </if>" +
            "<if test='endDateTime != null'> AND created_at &lt;= #{endDateTime} </if>" +
            "GROUP BY model_type" +
            "</script>")
    List<Map<String, Object>> getUserUsageByModel(@Param("userId") String userId,
                                                @Param("startDateTime") LocalDateTime startDateTime,
                                                @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * 获取用户每日用量趋势 - 这个需要自定义SQL或使用XML配置
     * 由于涉及日期函数和分组，这里保留原始的SQL查询方式
     */
    @Select("<script>" +
            "SELECT DATE(created_at) as date, " +
            "COUNT(*) as request_count, " +
            "SUM(input_tokens) as total_input_tokens, " +
            "SUM(output_tokens) as total_output_tokens, " +
            "SUM(input_cost + output_cost) as total_cost " +
            "FROM usage_record WHERE user_id = #{userId} " +
            "<if test='startDateTime != null'> AND created_at &gt;= #{startDateTime} </if>" +
            "<if test='endDateTime != null'> AND created_at &lt;= #{endDateTime} </if>" +
            "GROUP BY DATE(created_at) " +
            "ORDER BY DATE(created_at)" +
            "</script>")
    List<Map<String, Object>> getUserDailyUsageTrend(@Param("userId") String userId,
                                                   @Param("startDateTime") LocalDateTime startDateTime,
                                                   @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * 获取系统总消费
     */
    default BigDecimal getSystemTotalCost(LocalDateTime startDateTime,
                                 LocalDateTime endDateTime) {
        LambdaQueryWrapper<UsageRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(UsageRecord::getInputCost, UsageRecord::getOutputCost);
        
        if (startDateTime != null) {
            queryWrapper.ge(UsageRecord::getCreatedAt, startDateTime);
        }
        
        if (endDateTime != null) {
            queryWrapper.le(UsageRecord::getCreatedAt, endDateTime);
        }
        
        List<Map<String, Object>> maps = this.selectMaps(queryWrapper);
        
        BigDecimal totalCost = BigDecimal.ZERO;
        for (Map<String, Object> map : maps) {
            BigDecimal inputCost = (BigDecimal) map.get("input_cost");
            BigDecimal outputCost = (BigDecimal) map.get("output_cost");
            
            if (inputCost != null) {
                totalCost = totalCost.add(inputCost);
            }
            
            if (outputCost != null) {
                totalCost = totalCost.add(outputCost);
            }
        }
        
        return totalCost;
    }

    /**
     * 获取系统总输入token数
     */
    default Integer getSystemTotalInputTokens(LocalDateTime startDateTime,
                                     LocalDateTime endDateTime) {
        LambdaQueryWrapper<UsageRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(UsageRecord::getInputTokens);
        
        if (startDateTime != null) {
            queryWrapper.ge(UsageRecord::getCreatedAt, startDateTime);
        }
        
        if (endDateTime != null) {
            queryWrapper.le(UsageRecord::getCreatedAt, endDateTime);
        }
        
        List<UsageRecord> records = this.selectList(queryWrapper);
        
        return records.stream()
                .mapToInt(UsageRecord::getInputTokens)
                .sum();
    }

    /**
     * 获取系统总输出token数
     */
    default Integer getSystemTotalOutputTokens(LocalDateTime startDateTime,
                                      LocalDateTime endDateTime) {
        LambdaQueryWrapper<UsageRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(UsageRecord::getOutputTokens);
        
        if (startDateTime != null) {
            queryWrapper.ge(UsageRecord::getCreatedAt, startDateTime);
        }
        
        if (endDateTime != null) {
            queryWrapper.le(UsageRecord::getCreatedAt, endDateTime);
        }
        
        List<UsageRecord> records = this.selectList(queryWrapper);
        
        return records.stream()
                .mapToInt(UsageRecord::getOutputTokens)
                .sum();
    }

    /**
     * 获取系统总请求次数
     */
    default Long getSystemTotalRequests(LocalDateTime startDateTime,
                               LocalDateTime endDateTime) {
        LambdaQueryWrapper<UsageRecord> queryWrapper = new LambdaQueryWrapper<>();
        
        if (startDateTime != null) {
            queryWrapper.ge(UsageRecord::getCreatedAt, startDateTime);
        }
        
        if (endDateTime != null) {
            queryWrapper.le(UsageRecord::getCreatedAt, endDateTime);
        }
        
        return this.selectCount(queryWrapper);
    }

    /**
     * 获取活跃用户数 - 需要使用distinct，这里需要使用自定义SQL查询
     */
    @Select("<script>" +
            "SELECT COUNT(DISTINCT user_id) FROM usage_record " +
            "<where>" +
            "<if test='startDateTime != null'> created_at &gt;= #{startDateTime} </if>" +
            "<if test='endDateTime != null'> AND created_at &lt;= #{endDateTime} </if>" +
            "</where>" +
            "</script>")
    Long getActiveUserCount(@Param("startDateTime") LocalDateTime startDateTime,
                           @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * 获取按模型分组的系统用量 - 这个需要自定义SQL或使用XML配置
     * 由于涉及分组聚合，这里保留原始的SQL查询方式
     */
    @Select("<script>" +
            "SELECT model_type, " +
            "COUNT(*) as request_count, " +
            "SUM(input_tokens) as total_input_tokens, " +
            "SUM(output_tokens) as total_output_tokens, " +
            "SUM(input_cost + output_cost) as total_cost " +
            "FROM usage_record " +
            "<where>" +
            "<if test='startDateTime != null'> created_at &gt;= #{startDateTime} </if>" +
            "<if test='endDateTime != null'> AND created_at &lt;= #{endDateTime} </if>" +
            "</where>" +
            "GROUP BY model_type" +
            "</script>")
    List<Map<String, Object>> getSystemUsageByModel(@Param("startDateTime") LocalDateTime startDateTime,
                                                  @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * 获取系统每日用量趋势 - 这个需要自定义SQL或使用XML配置
     * 由于涉及日期函数和分组，这里保留原始的SQL查询方式
     */
    @Select("<script>" +
            "SELECT DATE(created_at) as date, " +
            "COUNT(*) as request_count, " +
            "SUM(input_tokens) as total_input_tokens, " +
            "SUM(output_tokens) as total_output_tokens, " +
            "SUM(input_cost + output_cost) as total_cost " +
            "FROM usage_record " +
            "<where>" +
            "<if test='startDateTime != null'> created_at &gt;= #{startDateTime} </if>" +
            "<if test='endDateTime != null'> AND created_at &lt;= #{endDateTime} </if>" +
            "</where>" +
            "GROUP BY DATE(created_at) " +
            "ORDER BY DATE(created_at)" +
            "</script>")
    List<Map<String, Object>> getSystemDailyUsageTrend(@Param("startDateTime") LocalDateTime startDateTime,
                                                     @Param("endDateTime") LocalDateTime endDateTime);
}