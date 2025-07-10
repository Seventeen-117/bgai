package com.bgpay.bgai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

/**
 * 测试Logback配置是否正常工作
 */
public class LogbackTest {
    private static final Logger log = LoggerFactory.getLogger(LogbackTest.class);

    @Test
    public void testLogback() {
        log.info("测试Logback配置是否正常工作");
        log.debug("这是一条调试日志");
        log.warn("这是一条警告日志");
        log.error("这是一条错误日志");
    }
} 