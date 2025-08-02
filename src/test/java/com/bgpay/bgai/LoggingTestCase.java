package com.bgpay.bgai;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * 简单的日志测试案例
 * 用于验证日志配置是否正常工作
 */
@SpringBootTest(
    classes = {}, // 不加载任何主应用类
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.main.allow-circular-references=true",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration,org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration,io.seata.spring.boot.autoconfigure.SeataAutoConfiguration,org.springframework.cloud.gateway.config.GatewayAutoConfiguration"
    }
)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.cloud.nacos.config.enabled=false",
    "spring.cloud.nacos.discovery.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "spring.cloud.config.enabled=false",
    "spring.cloud.consul.enabled=false",
    "spring.cloud.gateway.enabled=false",
    "spring.data.elasticsearch.repositories.enabled=false",
    "spring.elasticsearch.enabled=false",
    "seata.enabled=false",
    "rocketmq.enable=false",
    "cache.warmer.enabled=false",
    "bgai.feign.enabled=false",
    "gateway.enabled=false"
})
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