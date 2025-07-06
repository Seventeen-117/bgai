package com.bgpay.bgai.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/**
 * TestNG响应式测试基类
 * 专门用于WebFlux和响应式测试，支持随机端口
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
public abstract class AbstractReactiveTestNGTest extends AbstractTestNGSpringContextTests {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    /**
     * 测试类初始化前的通用设置
     */
    @BeforeClass
    public void setUp() {
        log.info("初始化响应式测试类: {}", this.getClass().getSimpleName());
        
        // 设置为WebFlux模式
        System.setProperty("spring.main.web-application-type", "reactive");
        System.setProperty("springdoc.api-docs.enabled", "false");
        System.setProperty("springdoc.swagger-ui.enabled", "false");
        
        // 确保配置参数已设置
        ensureConfigParams();
    }
    
    /**
     * 每个测试方法执行前的处理
     */
    @BeforeMethod
    public void beforeTestMethod() {
        log.info("----------------------------------------");
        log.info("开始执行测试方法");
    }
    
    /**
     * 确保必要的配置参数已经设置
     */
    private void ensureConfigParams() {
        // 检查是否设置了必要的系统属性
        String[] requiredProps = {
            "spring.profiles.active",
            "spring.cloud.nacos.config.server-addr",
            "spring.cloud.nacos.config.namespace"
        };
        
        for (String prop : requiredProps) {
            if (System.getProperty(prop) == null) {
                log.warn("缺少系统属性: {}，尝试从环境变量获取", prop);
                // 如果系统属性未设置，则尝试设置默认值
                setDefaultPropertyIfMissing(prop);
            }
        }
    }
    
    /**
     * 为缺失的属性设置默认值
     */
    private void setDefaultPropertyIfMissing(String propName) {
        switch (propName) {
            case "spring.profiles.active":
                System.setProperty(propName, "dev");
                break;
            case "spring.cloud.nacos.config.server-addr":
                System.setProperty(propName, "8.133.246.113:8848");
                break;
            case "spring.cloud.nacos.config.namespace":
                System.setProperty(propName, "d750d92e-152f-4055-a641-3bc9dda85a29");
                break;
        }
    }
} 