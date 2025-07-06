package com.bgpay.bgai;

import com.bgpay.bgai.config.MockRedisConfig;
import com.bgpay.bgai.config.NoLiquibaseTestConfig;
import com.bgpay.bgai.config.TestModuleExclusionConfig;
import com.bgpay.bgai.config.TestNacosConfig;
import com.bgpay.bgai.config.WebReactiveTestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * WebFlux专用测试启动类
 * 完全排除所有MVC相关配置和组件
 */
@SpringBootApplication(exclude = {
    WebMvcAutoConfiguration.class,
    ErrorMvcAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class,
    ElasticsearchDataAutoConfiguration.class,
    ElasticsearchRepositoriesAutoConfiguration.class
})
@ComponentScan(
    basePackages = "com.bgpay.bgai",
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ANNOTATION,
            classes = EnableWebMvc.class
        ),
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                BgaiApplication.class,
                TestRunner.class,
                TestAuthRunner.class
            }
        )
    }
)
@Import({
    WebReactiveTestConfig.class, 
    TestNacosConfig.class, 
    MockRedisConfig.class, 
    NoLiquibaseTestConfig.class, 
    TestModuleExclusionConfig.class
})
public class WebFluxOnlyTestRunner {
    private static final Logger log = LoggerFactory.getLogger(WebFluxOnlyTestRunner.class);
    
    public static void main(String[] args) {
        log.info("启动纯WebFlux测试运行器...");
        
        // 设置系统属性
        System.setProperty("spring.main.web-application-type", "reactive");
        System.setProperty("spring.main.allow-bean-definition-overriding", "true");
        System.setProperty("spring.webflux.mvc-exclusion-prevent", "false");
        
        // 显式禁用MVC相关配置
        System.setProperty("spring.mvc.enabled", "false");
        System.setProperty("spring.mvc.servlet.load-on-startup", "-1");
        
        // 显式启用WebFlux
        System.setProperty("spring.webflux.enabled", "true");
        
        // 禁用不需要的自动配置
        System.setProperty("spring.autoconfigure.exclude", 
            WebMvcAutoConfiguration.class.getName() + "," + 
            ErrorMvcAutoConfiguration.class.getName());
        
        // 使用WebApplicationType.REACTIVE强制使用WebFlux
        new SpringApplicationBuilder(WebFluxOnlyTestRunner.class)
            .web(WebApplicationType.REACTIVE)
            .run(args);
    }
} 