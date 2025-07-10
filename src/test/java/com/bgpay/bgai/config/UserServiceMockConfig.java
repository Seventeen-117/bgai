package com.bgpay.bgai.config;

import com.bgpay.bgai.service.UserService;
import com.bgpay.bgai.service.impl.UserServiceImpl;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * UserService Mock配置类
 * 提供测试环境使用的UserService实现
 */
@TestConfiguration
public class UserServiceMockConfig {

    /**
     * 提供UserService mock
     */
    @Primary
    @Bean
    @ConditionalOnMissingBean(UserService.class)
    public UserService userService() {
        return Mockito.mock(UserService.class);
    }
    
    /**
     * 提供UserServiceImpl mock
     * 用于解决UserService有多个实现的问题
     * userServiceImpl名称与UserServiceImpl类创建的默认bean名称相同
     */
    @Bean
    @ConditionalOnMissingBean(UserServiceImpl.class)
    public UserServiceImpl userServiceImpl() {
        return Mockito.mock(UserServiceImpl.class);
    }
} 