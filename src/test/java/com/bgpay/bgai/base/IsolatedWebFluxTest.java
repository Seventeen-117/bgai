package com.bgpay.bgai.base;

import com.bgpay.bgai.IsolatedWebFluxTestRunner;
import com.bgpay.bgai.config.IsolatedWebFluxTestConfig;
import com.bgpay.bgai.config.MockRedisConfig;
import com.bgpay.bgai.config.NoLiquibaseTestConfig;
import com.bgpay.bgai.config.TestModuleExclusionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;

/**
 * 完全隔离的WebFlux测试基类
 * 确保不会加载任何MVC相关配置
 */
@SpringBootTest(
    classes = IsolatedWebFluxTestRunner.class, 
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.main.web-application-type=reactive",
        "spring.mvc.enabled=false",
        "spring.webflux.enabled=true"
    }
)
@Import({
    IsolatedWebFluxTestConfig.class, 
    MockRedisConfig.class, 
    NoLiquibaseTestConfig.class, 
    TestModuleExclusionConfig.class
})
@EnableAutoConfiguration(exclude = {
    WebMvcAutoConfiguration.class,
    ErrorMvcAutoConfiguration.class
})
@TestPropertySource(locations = "classpath:application-webflux-test.yml")
@ActiveProfiles("dev")
public abstract class IsolatedWebFluxTest extends AbstractTestNGSpringContextTests {
    protected static final Logger log = LoggerFactory.getLogger(IsolatedWebFluxTest.class);
    
    @BeforeClass
    public void setUpIsolatedWebFluxTest() {
        log.info("初始化隔离的WebFlux测试基础环境");
        
        // 设置系统属性，确保使用WebFlux
        System.setProperty("spring.main.web-application-type", "reactive");
        System.setProperty("spring.main.allow-bean-definition-overriding", "true");
        System.setProperty("spring.webflux.mvc-exclusion-prevent", "false");
        
        // 显式禁用MVC相关配置
        System.setProperty("spring.mvc.enabled", "false");
        System.setProperty("spring.mvc.servlet.load-on-startup", "-1");
        
        // 显式启用WebFlux
        System.setProperty("spring.webflux.enabled", "true");
        
        // 禁用不必要的功能
        System.setProperty("springdoc.api-docs.enabled", "false");
        System.setProperty("springdoc.swagger-ui.enabled", "false");
        System.setProperty("seata.enabled", "false");
        System.setProperty("seata.saga.state-machine.auto-register", "false");
        System.setProperty("spring.liquibase.enabled", "false");
    }
} 