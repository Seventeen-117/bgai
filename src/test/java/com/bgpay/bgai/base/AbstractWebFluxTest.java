package com.bgpay.bgai.base;

import com.bgpay.bgai.WebFluxOnlyTestRunner;
import com.bgpay.bgai.config.MockRepositoryConfig;
import com.bgpay.bgai.config.NoLiquibaseTestConfig;
import com.bgpay.bgai.config.TestModuleExclusionConfig;
import com.bgpay.bgai.config.TestNacosConfig;
import com.bgpay.bgai.config.WebReactiveTestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;

/**
 * WebFlux响应式测试专用基类
 * 设置基本的Spring Boot测试环境，专为WebFlux配置
 */
@SpringBootTest(classes = WebFluxOnlyTestRunner.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
    WebReactiveTestConfig.class, 
    TestNacosConfig.class, 
    MockRepositoryConfig.class, 
    NoLiquibaseTestConfig.class, 
    TestModuleExclusionConfig.class
})
@EnableAutoConfiguration(exclude = {
    WebMvcAutoConfiguration.class,
    ErrorMvcAutoConfiguration.class
})
@TestPropertySource(properties = {
    "spring.main.web-application-type=reactive",
    "spring.main.allow-bean-definition-overriding=true",
    "spring.webflux.mvc-exclusion-prevent=false",
    "spring.mvc.enabled=false",
    "spring.mvc.servlet.load-on-startup=-1",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration"
})
public abstract class AbstractWebFluxTest extends AbstractTestNGSpringContextTests {
    protected static final Logger log = LoggerFactory.getLogger(AbstractWebFluxTest.class);
    
    @BeforeClass
    public void setUpWebFluxTest() {
        log.info("初始化WebFlux测试基础环境");
        
        // 设置系统属性，确保使用WebFlux
        System.setProperty("spring.main.web-application-type", "reactive");
        System.setProperty("spring.main.allow-bean-definition-overriding", "true");
        System.setProperty("spring.webflux.mvc-exclusion-prevent", "false");
        
        // 显式禁用MVC相关配置
        System.setProperty("spring.mvc.enabled", "false");
        System.setProperty("spring.mvc.servlet.load-on-startup", "-1");
        
        // 禁用不必要的功能
        System.setProperty("springdoc.api-docs.enabled", "false");
        System.setProperty("springdoc.swagger-ui.enabled", "false");
        System.setProperty("seata.enabled", "false");
        System.setProperty("seata.saga.state-machine.auto-register", "false");
        System.setProperty("spring.liquibase.enabled", "false");
    }
} 