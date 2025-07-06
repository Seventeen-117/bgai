package com.bgpay.bgai;

import com.bgpay.bgai.config.MockRepositoryConfig;
import com.bgpay.bgai.config.NoLiquibaseTestConfig;
import com.bgpay.bgai.config.TestModuleExclusionConfig;
import com.bgpay.bgai.config.TestNacosConfig;
import com.bgpay.bgai.config.WebReactiveTestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;

/**
 * WebFlux测试专用启动类
 * 避免加载WebMVC相关的配置
 */
@SpringBootApplication(exclude = {
    WebMvcAutoConfiguration.class,
    ErrorMvcAutoConfiguration.class
})
@Import({
    WebReactiveTestConfig.class, 
    com.bgpay.bgai.config.TestNacosConfig.class, 
    MockRepositoryConfig.class, 
    NoLiquibaseTestConfig.class, 
    TestModuleExclusionConfig.class
})
public class WebFluxTestRunner {
    private static final Logger log = LoggerFactory.getLogger(WebFluxTestRunner.class);
    
    public static void main(String[] args) {
        log.info("启动WebFlux测试运行器...");
        
        // 设置系统属性
        System.setProperty("spring.main.web-application-type", "reactive");
        System.setProperty("spring.main.allow-bean-definition-overriding", "true");
        System.setProperty("spring.webflux.mvc-exclusion-prevent", "false");
        
        // 显式禁用MVC相关配置
        System.setProperty("spring.mvc.enabled", "false");
        System.setProperty("spring.mvc.servlet.load-on-startup", "-1");
        
        // 禁用不需要的自动配置
        System.setProperty("spring.autoconfigure.exclude", 
            WebMvcAutoConfiguration.class.getName() + "," + 
            ErrorMvcAutoConfiguration.class.getName());
        
        // 使用WebApplicationType.REACTIVE强制使用WebFlux
        new SpringApplicationBuilder(WebFluxTestRunner.class)
            .web(WebApplicationType.REACTIVE)
            .run(args);
    }
} 