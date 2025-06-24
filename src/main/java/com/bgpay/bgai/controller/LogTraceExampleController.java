package com.bgpay.bgai.controller;

import com.bgpay.bgai.utils.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 日志追踪示例控制器
 * 用于演示LogTraceInterceptor的功能
 */
@RestController
@RequestMapping("/api/log-trace")
public class LogTraceExampleController {
    private static final Logger logger = LoggerFactory.getLogger(LogTraceExampleController.class);

    /**
     * 演示同步API中的日志追踪
     * @param message 要记录的消息
     * @return 包含追踪信息的响应
     */
    @GetMapping("/example")
    public Map<String, String> logTraceExample(@RequestParam(defaultValue = "测试消息") String message) {
        // 日志中会自动包含traceId和userId (如果已设置)
        logger.info("接收到请求消息: {}", message);
        
        // 业务处理...
        logger.debug("处理业务逻辑...");
        
        // 模拟一些警告级别的日志
        if (message.length() > 10) {
            logger.warn("消息长度超过10个字符: {}", message.length());
        }
        
        // 返回追踪信息
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        response.put("traceId", LogUtils.getTraceId());
        response.put("userId", LogUtils.getUserId() != null ? LogUtils.getUserId() : "anonymous");
        
        logger.info("请求处理完成，返回响应");
        return response;
    }
} 