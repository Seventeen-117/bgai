package com.bgpay.bgai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("allowed_file_type")
public class AllowedFileType {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String mimeType;
    private Boolean enabled;
}