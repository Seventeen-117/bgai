package com.bgpay.bgai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
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
 * @since 2025-03-10 15:32:02
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("price_version")
@ApiModel(value = "PriceVersion对象", description = "")
public class PriceVersion {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("model_id")
    private Integer modelId;

    @TableField("model_type") // 添加 modelType 字段
    private String modelType;

    @TableField("version")
    @Version
    private Integer version;

    @TableField("effective_date")
    private LocalDateTime effectiveDate;

    @TableField("is_current")
    private Boolean isCurrent;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
