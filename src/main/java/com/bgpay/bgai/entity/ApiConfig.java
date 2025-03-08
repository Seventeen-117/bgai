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

/**
 * <p>
 * 
 * </p>
 *
 * @author zly
 * @since 2025-03-08 20:03:01
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("api_config")
@ApiModel(value = "ApiConfig对象", description = "")
public class ApiConfig {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("api_url")
    private String apiUrl;

    @TableField("api_key")
    private String apiKey;

    @TableField("model_name")
    private String modelName;

    @TableField("user_id")
    private String userId;
}
