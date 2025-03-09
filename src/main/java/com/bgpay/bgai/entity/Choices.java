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
 * @since 2025-03-08 23:37:28
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("choices")
@ApiModel(value = "Choices对象", description = "")
public class Choices {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("chat_completion_id")
    private String chatCompletionId;

    @TableField("index_num")
    private Integer indexNum;

    @TableField("finish_reason")
    private String finishReason;

}
