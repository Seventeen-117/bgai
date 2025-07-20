package com.bgpay.bgai.config;

import com.bgpay.bgai.controller.AuthController;
import com.bgpay.bgai.controller.ApiKeyController;
import com.bgpay.bgai.controller.DynamicRouteController;
import com.bgpay.bgai.controller.SystemConfigController;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 提供MockMvc bean的配置类
 * 解决测试类中MockMvc依赖注入失败问题
 */
@TestConfiguration
public class MockMvcConfig {

    /**
     * 提供MockMvc bean
     * 使用standaloneSetup避免ServletContext依赖
     */
    @Bean
    @Primary
    public MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(
            Mockito.mock(AuthController.class),
            Mockito.mock(ApiKeyController.class),
            Mockito.mock(DynamicRouteController.class),
            Mockito.mock(SystemConfigController.class)
        ).build();
    }
} 