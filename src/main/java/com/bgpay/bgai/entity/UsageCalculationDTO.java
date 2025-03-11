package com.bgpay.bgai.entity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

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
    private int completionTokens;

    @PastOrPresent
    private LocalDateTime createdAt;
}