package com.bgpay.bgai.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
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
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

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
    private Integer promptCacheHitTokens = 0;

    @Min(0)
    private Integer promptCacheMissTokens = 0;

    @Min(0)
    private Integer promptTokensCached = 0;

    @Min(0)
    private Integer promptTokens = 0;

    @Min(0)
    private Integer completionReasoningTokens = 0;

    @Min(0)
    private Integer completionTokens = 0;

    @PastOrPresent
    @JsonDeserialize(using = FlexibleDateTimeDeserializer.class)
    private LocalDateTime createdAt;

    @JsonDeserialize(using = FlexibleDateTimeDeserializer.class)
    private LocalDateTime updatedAt;

    @Min(0)
    private BigDecimal inputCost;

    @Min(0)
    private BigDecimal outputCost;

    // 新增
    private String messageId;

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
    
    /**
     * 灵活的日期时间反序列化器，支持多种格式
     */
    @Slf4j
    public static class FlexibleDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        private static final List<DateTimeFormatter> FORMATTERS = Arrays.asList(
            // ISO-8601 格式带Z时区标识符
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"),
            // 标准ISO格式
            DateTimeFormatter.ISO_DATE_TIME,
            // 常规格式
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            // 简化格式
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
        );

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            String dateTimeStr = p.getText().trim();
            
            if (dateTimeStr == null || dateTimeStr.isEmpty()) {
                return null;
            }
            
            log.debug("Attempting to parse datetime: {}", dateTimeStr);
            
            // 检查是否带有Z时区结尾
            if (dateTimeStr.endsWith("Z")) {
                try {
                    // 使用Instant处理带时区的ISO-8601格式
                    Instant instant = Instant.parse(dateTimeStr);
                    return LocalDateTime.ofInstant(instant, ZoneId.of("GMT+8"));
                } catch (DateTimeParseException e) {
                    log.debug("Could not parse as Instant: {}", e.getMessage());
                }
            }
            
            // 尝试使用预定义的格式
            for (DateTimeFormatter formatter : FORMATTERS) {
                try {
                    return LocalDateTime.parse(dateTimeStr, formatter);
                } catch (DateTimeParseException e) {
                    // 继续尝试下一个格式
                    log.debug("Format {} failed: {}", formatter, e.getMessage());
                }
            }
            
            // 所有格式都失败，抛出异常
            throw new DateTimeParseException("Could not parse datetime with any of the predefined formats", dateTimeStr, 0);
        }
    }
}