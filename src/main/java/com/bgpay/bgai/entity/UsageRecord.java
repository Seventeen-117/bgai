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
 * 
 * </p>
 *
 * @author zly
 * @since 2025-03-09 21:17:29
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("usage_record")
@ApiModel(value = "UsageRecord对象", description = "")
public class UsageRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("model_id")
    private Integer modelId;

    @TableField("chat_completion_id")
    private String chatCompletionId;

    @TableField("input_cost")
    private BigDecimal inputCost;

    @TableField("output_cost")
    private BigDecimal outputCost;

    @TableField("price_version")
    private Integer priceVersion;

    @TableField("calculated_at")
    private LocalDateTime calculatedAt;
}
