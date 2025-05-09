package com.bgpay.bgai.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简化版的聊天响应对象，专门为/api/chatGatWay接口设计
 * 只包含响应内容，不包含用量信息，使响应更加简洁
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // 只序列化非空字段
public class SimpleChatResponse {
    /**
     * 响应内容
     */
    private String content;
    
    /**
     * 错误信息，如果有的话
     */
    private ErrorInfo error;
    
    /**
     * 从完整ChatResponse创建简化版响应
     */
    public static SimpleChatResponse fromChatResponse(ChatResponse chatResponse) {
        if (chatResponse == null) {
            return SimpleChatResponse.builder()
                    .error(new ErrorInfo(500, "服务内部错误"))
                    .build();
        }
        
        String content = chatResponse.getContent();
        
        // 处理空内容
        if (content == null || content.trim().isEmpty()) {
            return SimpleChatResponse.builder()
                    .error(new ErrorInfo(500, "未获取到有效的响应内容"))
                    .build();
        }
        
        // 检查内容是否为错误格式的JSON
        if (content.startsWith("{\"error\":")) {
            try {
                // 简单解析错误JSON
                String codeStr = content.substring(content.indexOf("\"code\":") + 7, content.indexOf(","));
                String message = content.substring(content.indexOf("\"message\":\"") + 11, content.lastIndexOf("\""));
                int code = Integer.parseInt(codeStr);
                
                return SimpleChatResponse.builder()
                        .error(new ErrorInfo(code, message))
                        .build();
            } catch (Exception e) {
                // 解析失败，直接返回内容
                return SimpleChatResponse.builder().content(content).build();
            }
        }
        
        // 处理内容中可能存在的特殊格式
        if (content.startsWith("发生错误:") || content.startsWith("服务暂时不可用")) {
            return SimpleChatResponse.builder()
                    .error(new ErrorInfo(500, content))
                    .build();
        }
        
        // 正常内容处理
        return SimpleChatResponse.builder().content(content).build();
    }
    
    /**
     * 创建一个包含错误信息的响应
     */
    public static SimpleChatResponse ofError(int code, String message) {
        return SimpleChatResponse.builder()
                .error(new ErrorInfo(code, message))
                .build();
    }
    
    /**
     * 创建一个包含内容的响应
     */
    public static SimpleChatResponse ofContent(String content) {
        return SimpleChatResponse.builder()
                .content(content)
                .build();
    }
    
    /**
     * 错误信息对象
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorInfo {
        private int code;
        private String message;
    }
} 