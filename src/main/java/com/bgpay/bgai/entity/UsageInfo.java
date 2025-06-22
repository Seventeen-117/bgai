package com.bgpay.bgai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author zly
 * @since 2025-03-08 23:09:50
 */
@Data
@TableName("usage_info")
@ApiModel(value = "UsageInfo对象", description = "")
public class UsageInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    @TableField("chat_completion_id")
    private String chatCompletionId;

    @TableField("prompt_tokens")
    private Integer promptTokens;

    @TableField("completion_tokens")
    private Integer completionTokens;

    @TableField("total_tokens")
    private Integer totalTokens;

    @TableField("prompt_tokens_cached")
    private Integer promptTokensCached;

    @TableField("completion_reasoning_tokens")
    private Integer completionReasoningTokens;

    @TableField("prompt_cache_hit_tokens")
    private Integer promptCacheHitTokens;

    @TableField("prompt_cache_miss_tokens")
    private Integer promptCacheMissTokens;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @TableField("model_type")
    private String modelType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
