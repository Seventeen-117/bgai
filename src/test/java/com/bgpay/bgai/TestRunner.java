package com.bgpay.bgai;

import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import com.bgpay.bgai.controller.AuthControllerTest;

/**
 * 测试运行器
 * 使用TestNG直接运行特定的测试类，绕过Maven
 */
public class TestRunner {
    public static void main(String[] args) {
        // 设置系统属性
        System.setProperty("spring.main.web-application-type", "servlet");
        System.setProperty("springdoc.api-docs.enabled", "false");
        System.setProperty("springdoc.swagger-ui.enabled", "false");
        
        // 创建TestNG实例
        TestNG testNG = new TestNG();
        
        // 设置监听器
        TestListenerAdapter tla = new TestListenerAdapter();
        testNG.addListener(tla);
        
        // 设置要运行的测试类
        testNG.setTestClasses(new Class[] { AuthControllerTest.class });
        
        // 设置运行参数
        testNG.setVerbose(2);
        
        // 运行测试
        testNG.run();
    }
} 