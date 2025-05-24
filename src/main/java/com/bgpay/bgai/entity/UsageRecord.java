package com.bgpay.bgai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 使用记录实体类
 * </p>
 *
 * @author zly
 * @since 2025-03-09 21:17:29
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("usage_record")
@ApiModel(value = "UsageRecord对象", description = "使用记录")
public class UsageRecord {

    @ApiModelProperty("主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("模型类型")
    @TableField("model_type")
    private String modelType;

    @ApiModelProperty("聊天完成ID")
    @TableField("chat_completion_id")
    private String chatCompletionId;

    @ApiModelProperty("用户ID")
    @TableField("user_id")
    private String userId;

    @ApiModelProperty("输入成本")
    @TableField("input_cost")
    private BigDecimal inputCost;

    @ApiModelProperty("输出成本")
    @TableField("output_cost")
    private BigDecimal outputCost;

    @ApiModelProperty("价格版本")
    @TableField("price_version")
    private Integer priceVersion;

    @ApiModelProperty("计算时间")
    @TableField("calculated_at")
    private LocalDateTime calculatedAt;

    @ApiModelProperty("消息ID")
    @TableField("message_id")
    private String messageId;

    @ApiModelProperty("输入token数")
    @TableField("input_tokens")
    private Integer inputTokens;

    @ApiModelProperty("输出token数")
    @TableField("output_tokens")
    private Integer outputTokens;

    @ApiModelProperty("状态")
    @TableField("status")
    private String status;

    @ApiModelProperty("创建时间")
    @TableField("created_at")
    private LocalDateTime createdAt;

    @ApiModelProperty("更新时间")
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
