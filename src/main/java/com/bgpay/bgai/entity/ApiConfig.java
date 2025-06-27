package com.bgpay.bgai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * API配置实体类
 * </p>
 *
 * @author zly
 * @since 2025-03-08 20:03:01
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("api_config")
@ApiModel(value = "ApiConfig对象", description = "API配置信息")
public class ApiConfig {

    @ApiModelProperty("主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("API名称")
    @TableField("config_name")
    private String name;

    @ApiModelProperty("API地址")
    @TableField("api_url")
    private String apiUrl;

    @ApiModelProperty("API密钥")
    @TableField("api_key")
    private String apiKey;

    @ApiModelProperty("模型名称")
    @TableField("model_name")
    private String modelName;

    @ApiModelProperty("模型类型")
    @TableField("model_type")
    private String modelType;

    @ApiModelProperty("用户ID")
    @TableField("user_id")
    private String userId;

    @ApiModelProperty("是否启用")
    @TableField("enabled")
    private Boolean enabled;

    @ApiModelProperty("是否默认")
    @TableField("is_default")
    private Boolean isDefault;

    @ApiModelProperty("优先级")
    @TableField("priority")
    private Integer priority;

    @ApiModelProperty("描述")
    @TableField("description")
    private String description;

    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    @TableField("update_time")
    private LocalDateTime updateTime;
}
