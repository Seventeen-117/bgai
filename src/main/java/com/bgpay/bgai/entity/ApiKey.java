package com.bgpay.bgai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("api_key")
public class ApiKey {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String apiKey;
    private String clientId;
    private String clientName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Integer active;
} 