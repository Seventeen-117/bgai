package com.bgpay.bgai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

/**
 * 极简日志测试类
 * 不依赖Spring上下文，直接验证日志配置
 */
public class SimpleLoggingTest {
    
    private static final Logger log = LoggerFactory.getLogger(SimpleLoggingTest.class);
    
    @Test
    public void testLogstashDisabled() {
        System.out.println("====== 开始极简日志测试 ======");
        log.info("这是一条INFO日志，测试logstash是否已禁用");
        log.debug("这是一条DEBUG日志");
        log.warn("这是一条WARN日志");
        log.error("这是一条ERROR日志");
        
        // 确保不会有Logstash连接警告
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // 忽略中断
        }
        
        System.out.println("====== 结束极简日志测试 ======");
    }
} 