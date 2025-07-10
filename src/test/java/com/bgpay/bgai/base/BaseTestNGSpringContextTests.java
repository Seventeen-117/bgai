package com.bgpay.bgai.base;

import com.bgpay.bgai.BgaiApplication;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 基础测试类，继承Spring的AbstractTestNGSpringContextTests
 * 提供通用的测试功能和生命周期方法
 */
@SpringBootTest(
    classes = BgaiApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.main.allow-bean-definition-overriding=true"
    }
)
@TestPropertySource(properties = {
    "seata.enabled=false",
    "saga.enabled=false",
    "seata.saga.state-machine.auto-register=false"
})
public abstract class BaseTestNGSpringContextTests extends AbstractTestNGSpringContextTests {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected String testId;
    
    /**
     * 在测试类初始化时执行
     */
    @BeforeClass(alwaysRun = true)
    public void setUpClass() {
        log.info("初始化测试类: {}", getClass().getSimpleName());
        Allure.label("testClass", getClass().getSimpleName());
        
        // 禁用Seata
        System.setProperty("seata.enabled", "false");
        System.setProperty("saga.enabled", "false");
        System.setProperty("seata.saga.state-machine.auto-register", "false");
    }
    
    /**
     * 在每个测试方法执行前执行
     */
    @BeforeMethod(alwaysRun = true)
    public void setUpMethod(Method method) {
        testId = UUID.randomUUID().toString();
        log.info("开始执行测试方法: {}, testId: {}", method.getName(), testId);
        Allure.parameter("testId", testId);
        Allure.parameter("testMethod", method.getName());
    }
    
    /**
     * 在每个测试方法执行后执行
     */
    @AfterMethod(alwaysRun = true)
    public void tearDownMethod(Method method) {
        log.info("测试方法执行完成: {}, testId: {}", method.getName(), testId);
    }
    
    /**
     * 记录测试步骤（用于Allure报告）
     */
    protected void logStep(String stepDescription) {
        log.info("测试步骤: {}", stepDescription);
        Allure.step(stepDescription);
    }
    
    /**
     * 添加测试附件（用于Allure报告）
     */
    protected void addAttachment(String name, String content) {
        log.debug("添加测试附件: {}", name);
        Allure.addAttachment(name, content);
    }
    
    /**
     * 添加测试附件（用于Allure报告）
     */
    protected void addAttachment(String name, String contentType, String content) {
        log.debug("添加测试附件: {}, 类型: {}", name, contentType);
        Allure.addAttachment(name, contentType, content);
    }
} 