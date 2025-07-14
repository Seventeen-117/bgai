package com.bgpay.bgai.response;

import com.alibaba.dashscope.threads.runs.Usage;
import com.bgpay.bgai.entity.UsageInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // 排除所有为null的字段
public final class ChatResponse {
    private String content;
    private UsageInfo usage;
    private boolean success = true;
    private Error error;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Error {
        private int code;
        private String message;
    }
}