package com.bgpay.bgai;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * 简单的日志测试案例
 * 用于验证日志配置是否正常工作
 */
@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class LoggingTestCase extends AbstractTestNGSpringContextTests {

    @Test
    public void testLogging() {
        log.info("======= 日志测试开始 =======");
        log.info("这条日志应该正常显示且不会有Logstash连接错误");
        log.debug("这是一条调试日志");
        log.warn("这是一条警告日志");
        log.error("这是一条错误日志");
        log.info("======= 日志测试结束 =======");
    }
} 