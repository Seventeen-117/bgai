package com.bgpay.bgai.response;

import com.alibaba.dashscope.threads.runs.Usage;
import com.bgpay.bgai.entity.UsageInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
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