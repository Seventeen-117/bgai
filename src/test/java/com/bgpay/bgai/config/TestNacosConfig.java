package com.bgpay.bgai.config;

import com.alibaba.cloud.nacos.NacosConfigAutoConfiguration;
import com.alibaba.cloud.nacos.NacosConfigProperties;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.testng.annotations.BeforeSuite;

/**
 * TestNG 与 Nacos 集成配置类
 * 用于在测试环境中自动从Nacos加载配置，与实际运行环境保持一致
 */
@TestConfiguration
@RefreshScope
public class TestNacosConfig {
    private static final Logger log = LoggerFactory.getLogger(TestNacosConfig.class);

    @Value("${spring.cloud.nacos.config.server-addr:8.133.246.113:8848}")
    private String serverAddr;

    @Value("${spring.cloud.nacos.config.namespace:d750d92e-152f-4055-a641-3bc9dda85a29}")
    private String namespace;

    @Value("${spring.cloud.nacos.config.group:DEFAULT_GROUP}")
    private String group;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 在测试套件启动前初始化Nacos配置
     */
    @BeforeSuite
    public void setupNacosConfig() {
        log.info("初始化TestNG Nacos配置环境");
        log.info("Nacos服务器: {}, 命名空间: {}, 环境: {}", serverAddr, namespace, activeProfile);

        // 设置系统属性，确保所有测试类都能访问相同的配置
        System.setProperty("spring.cloud.nacos.config.server-addr", serverAddr);
        System.setProperty("spring.cloud.nacos.config.namespace", namespace);
        System.setProperty("spring.cloud.nacos.config.group", group);
        System.setProperty("spring.profiles.active", activeProfile);
        
        // 禁用Seata Saga状态机自动注册，避免重复注册错误
        System.setProperty("seata.saga.state-machine.auto-register", "false");
        
        // 禁用Micrometer Metrics，避免关闭时的bean创建错误
        System.setProperty("management.simple.metrics.export.enabled", "false");
        System.setProperty("management.metrics.enable.all", "false");
    }

    /**
     * 创建Nacos配置属性
     */
    @Bean
    @Primary
    public NacosConfigProperties nacosConfigProperties(Environment environment) {
        NacosConfigProperties properties = new NacosConfigProperties();
        properties.setServerAddr(serverAddr);
        properties.setNamespace(namespace);
        properties.setGroup(group);
        properties.setFileExtension("yaml");
        properties.setRefreshEnabled(true);
        properties.setTimeout(5000);

        log.info("创建测试环境Nacos配置属性: {}", properties);
        return properties;
    }

    /**
     * 提供配置异常处理器，防止Nacos连接问题影响测试
     */
    @Bean
    public NacosConfigExceptionHandler nacosExceptionHandler() {
        return new NacosConfigExceptionHandler();
    }

    /**
     * Nacos配置异常处理内部类
     */
    public static class NacosConfigExceptionHandler {
        private static final Logger log = LoggerFactory.getLogger(NacosConfigExceptionHandler.class);

        public NacosConfigExceptionHandler() {
            log.info("Nacos测试配置异常处理已启用，将使用本地配置回退");
        }

        /**
         * 处理Nacos配置异常
         */
        public void handleConfigException(NacosException e) {
            log.warn("Nacos配置加载异常，将使用本地配置: {}", e.getMessage());
        }
    }
} 