package com.bgpay.bgai.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UsageCalculationDTO {
    @NotBlank
    private String chatCompletionId;

    @Pattern(regexp = "chat|reasoner")
    private String modelType;

    @Min(0)
    private int promptCacheHitTokens;

    @Min(0)
    private int promptCacheMissTokens;

    @Min(0)
    private int promptTokensCached;

    @Min(0)
    private int promptTokens;

    @Min(0)
    private int completionReasoningTokens;

    @Min(0)
    private int completionTokens;

    @PastOrPresent
    private LocalDateTime createdAt;

    @Min(0)
    private BigDecimal inputCost;

    @Min(0)
    private BigDecimal outputCost;

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
}