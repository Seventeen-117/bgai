package com.bgpay.bgai.response;

import com.alibaba.dashscope.threads.runs.Usage;
import com.bgpay.bgai.entity.UsageInfo;
import lombok.Data;

import java.util.UUID;

@Data
public final class ChatResponse {
    private String content;
    private UsageInfo usage;
}