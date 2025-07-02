package com.bgpay.bgai;

import com.bgpay.bgai.controller.AuthControllerTest;
import com.bgpay.bgai.controller.SimpleAuthControllerTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 测试运行器
 * 可以直接在IDE中运行这个类来执行测试
 * 也可以指定测试类名和测试方法名作为参数运行
 */
public class TestAuthRunner {
    
    public static void main(String[] args) {
        System.out.println("开始运行认证测试...");
        
        // 创建测试监听器
        TestListenerAdapter tla = new TestListenerAdapter();
        TestNG testNG = new TestNG();
        
        // 根据参数确定测试运行模式
        if (args.length >= 1) {
            // 如果有参数，尝试以编程方式构建测试套件
            String testClassName = args[0];
            String testMethodName = args.length >= 2 ? args[1] : null;
            
            // 创建测试套件
            XmlSuite suite = new XmlSuite();
            suite.setName("Programmatic Auth Test Suite");
            
            XmlTest test = new XmlTest(suite);
            test.setName("Auth Test");
            
            // 创建测试类
            XmlClass xmlClass;
            try {
                if (testClassName.contains(".")) {
                    // 如果提供了完整类名
                    xmlClass = new XmlClass(testClassName);
                } else {
                    // 否则假设在controller包下
                    xmlClass = new XmlClass("com.bgpay.bgai.controller." + testClassName);
                }
            } catch (Exception e) {
                System.err.println("找不到指定的测试类: " + testClassName);
                e.printStackTrace();
                return;
            }
            
            // 如果指定了测试方法，添加到包含方法列表
            if (testMethodName != null && !testMethodName.isEmpty()) {
                List<XmlInclude> includes = new ArrayList<>();
                includes.add(new XmlInclude(testMethodName));
                xmlClass.setIncludedMethods(includes);
            }
            
            test.setXmlClasses(Arrays.asList(xmlClass));
            
            // 设置测试套件
            testNG.setXmlSuites(Arrays.asList(suite));
        } else {
            // 默认运行所有认证相关的测试
            try {
                // 尝试使用testng.xml
                testNG.setTestClasses(new Class[] {
                    SimpleAuthControllerTest.class,
                    AuthControllerTest.class
                });
            } catch (Exception e) {
                System.err.println("无法加载测试类，尝试使用配置文件...");
                // 回退到使用XML配置
                testNG.setTestSuites(Arrays.asList("src/test/resources/testng.xml"));
            }
        }
        
        testNG.addListener(tla);
        testNG.run();
        
        System.out.println("认证测试完成。");
        
        // 输出测试结果摘要
        System.out.println("通过: " + tla.getPassedTests().size());
        System.out.println("失败: " + tla.getFailedTests().size());
        System.out.println("跳过: " + tla.getSkippedTests().size());
        
        // 如果有失败的测试，打印详情
        if (tla.getFailedTests().size() > 0) {
            System.out.println("\n失败的测试:");
            tla.getFailedTests().forEach(testResult -> {
                System.out.println("  " + testResult.getTestClass().getName() + "." + testResult.getMethod().getMethodName());
                System.out.println("    - 错误信息: " + testResult.getThrowable().getMessage());
            });
        }
    }
} 