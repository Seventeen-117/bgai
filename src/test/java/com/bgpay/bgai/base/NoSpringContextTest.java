package com.bgpay.bgai.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestPropertySource;
import org.testng.annotations.BeforeClass;

/**
 * 无Spring上下文测试基类
 * 继承此类的测试不会加载任何Spring上下文
 * 适用于纯单元测试或使用Mockito的测试
 */
@TestPropertySource(properties = {
    "spring.main.web-application-type=none", 
    "spring.main.banner-mode=off",
    "spring.test.context.cache.maxSize=0"
})
public abstract class NoSpringContextTest {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    /**
     * 测试前禁用Spring环境
     */
    @BeforeClass(alwaysRun = true)
    public void disableSpringContext() {
        // 禁用Spring上下文加载
        System.setProperty("spring.main.web-application-type", "none");
        System.setProperty("spring.main.banner-mode", "off");
        System.setProperty("spring.test.context.cache.maxSize", "0");
        System.setProperty("spring.test.context.cache.enabled", "false");
        System.setProperty("spring.test.constructor.autowire.mode", "none");
        System.setProperty("spring.autoconfigure.exclude", 
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration," +
                "org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration," +
                "io.seata.spring.boot.autoconfigure.SeataAutoConfiguration," +
                "org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest");
        
        log.info("禁用Spring上下文加载");
    }
} 