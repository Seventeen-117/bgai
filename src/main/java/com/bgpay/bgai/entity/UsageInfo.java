package com.bgpay.bgai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author zly
 * @since 2025-03-08 23:09:50
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("usage_info")
@ApiModel(value = "UsageInfo对象", description = "")
public final class UsageInfo {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

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

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("model_type")
    private String modelType;

}
