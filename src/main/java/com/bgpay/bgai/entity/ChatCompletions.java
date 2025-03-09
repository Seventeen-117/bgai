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
 * @since 2025-03-08 23:37:43
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("chat_completions")
@ApiModel(value = "ChatCompletions对象", description = "")
public class ChatCompletions {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id; // 修改为 Long 类型

    @TableField("object")
    private String object;

    @TableField("created")
    private Long created;

    @TableField("model")
    private String model;

    @TableField("system_fingerprint")
    private String systemFingerprint;

    @TableField("api_key")
    private String apiKey;
}