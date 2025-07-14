package com.bgpay.bgai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * <p>
 * 用量信息实体类
 * </p>
 *
 * @author zly
 * @since 2025-03-08 23:09:50
 */
@Data
@TableName("usage_info")
@Schema(name = "UsageInfo", description = "用量信息数据")
@JsonInclude(JsonInclude.Include.NON_NULL) // 排除所有为null的字段
public class UsageInfo {

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "聊天完成ID")
    @TableField("chat_completion_id")
    private String chatCompletionId;

    @Schema(description = "提示词令牌数")
    @TableField("prompt_tokens")
    private Integer promptTokens;

    @Schema(description = "完成词令牌数")
    @TableField("completion_tokens")
    private Integer completionTokens;

    @Schema(description = "总令牌数")
    @TableField("total_tokens")
    private Integer totalTokens;

    @Schema(description = "缓存的提示词令牌数")
    @TableField("prompt_tokens_cached")
    private Integer promptTokensCached;

    @Schema(description = "完成推理令牌数")
    @TableField("completion_reasoning_tokens")
    private Integer completionReasoningTokens;

    @Schema(description = "提示词缓存命中令牌数")
    @TableField("prompt_cache_hit_tokens")
    private Integer promptCacheHitTokens;

    @Schema(description = "提示词缓存未命中令牌数")
    @TableField("prompt_cache_miss_tokens")
    private Integer promptCacheMissTokens;

    @Schema(description = "创建时间", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @Schema(description = "模型类型")
    @TableField("model_type")
    private String modelType;

    @Schema(description = "更新时间", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
