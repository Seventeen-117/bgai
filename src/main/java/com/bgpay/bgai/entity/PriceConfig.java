package com.bgpay.bgai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
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
 * @since 2025-03-10 13:30:40
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("price_config")
@ApiModel(value = "PriceConfig对象", description = "")
public class PriceConfig {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("模型类型")
    @TableField("model_type")
    private String modelType;

    @ApiModelProperty("时段类型")
    @TableField("time_period")
    private String timePeriod;

    @ApiModelProperty("缓存状态")
    @TableField("cache_status")
    private String cacheStatus;

    @ApiModelProperty("输入/输出类型")
    @TableField("io_type")
    private String ioType;

    @ApiModelProperty("单价")
    @TableField("price")
    private BigDecimal price;

    @ApiModelProperty("版本号")
    @TableField("version")
    @Version
    private Integer version;

    @ApiModelProperty("生效时间")
    @TableField("effective_time")
    private LocalDateTime effectiveTime;
}
