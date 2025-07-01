package com.bgpay.bgai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;

/**
 * TestNG Nacos配置监听器
 * 用于在测试套件启动前设置Nacos配置
 */
public class TestNacosConfigListener implements ISuiteListener {
    private static final Logger log = LoggerFactory.getLogger(TestNacosConfigListener.class);

    @Override
    public void onStart(ISuite suite) {
        log.info("测试套件[{}]开始执行，初始化Nacos配置", suite.getName());

        // 设置系统属性，确保所有测试类都能访问相同的配置
        System.setProperty("spring.cloud.nacos.config.server-addr", 
                System.getProperty("spring.cloud.nacos.config.server-addr", "8.133.246.113:8848"));
        
        System.setProperty("spring.cloud.nacos.config.namespace", 
                System.getProperty("spring.cloud.nacos.config.namespace", "d750d92e-152f-4055-a641-3bc9dda85a29"));
        
        System.setProperty("spring.cloud.nacos.config.group", 
                System.getProperty("spring.cloud.nacos.config.group", "DEFAULT_GROUP"));
        
        System.setProperty("spring.profiles.active", 
                System.getProperty("spring.profiles.active", "dev"));
        
        // 禁用Seata Saga状态机自动注册，避免重复注册错误
        System.setProperty("seata.saga.state-machine.auto-register", "false");
        
        // 禁用Micrometer Metrics，避免关闭时的bean创建错误
        System.setProperty("management.simple.metrics.export.enabled", "false");
        System.setProperty("management.metrics.enable.all", "false");
        
        log.info("Nacos配置初始化完成");
    }

    @Override
    public void onFinish(ISuite suite) {
        log.info("测试套件[{}]执行完成", suite.getName());
    }
} 