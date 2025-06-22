package com.bgpay.bgai.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UsageCalculationDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank
    private String chatCompletionId;

    // 新增
    private String userId;

    @Pattern(regexp = "chat|reasoner")
    private String modelType;

    @Min(0)
    private Integer promptCacheHitTokens;

    @Min(0)
    private Integer promptCacheMissTokens;

    @Min(0)
    private Integer promptTokensCached;

    @Min(0)
    private Integer promptTokens;

    @Min(0)
    private Integer completionReasoningTokens;

    @Min(0)
    private Integer completionTokens;

    @PastOrPresent
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;

    @Min(0)
    private BigDecimal inputCost;

    @Min(0)
    private BigDecimal outputCost;

    // 新增
    private String messageId;

    public BigDecimal getInputCost() {
        if (inputCost == null) {
            // 默认计算逻辑，可以根据实际需求调整
            return BigDecimal.valueOf(promptCacheHitTokens + promptCacheMissTokens)
                    .multiply(BigDecimal.valueOf(0.001)); // 示例：每1000个token收费1元
        }
        return inputCost;
    }

    public BigDecimal getOutputCost() {
        if (outputCost == null) {
            // 默认计算逻辑，可以根据实际需求调整
            return BigDecimal.valueOf(completionTokens)
                    .multiply(BigDecimal.valueOf(0.002)); // 示例：每1000个token收费2元
        }
        return outputCost;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessageId() {
        return messageId;
    }
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}