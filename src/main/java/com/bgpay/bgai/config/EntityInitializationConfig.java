package com.bgpay.bgai.config;

import com.bgpay.bgai.entity.UserToken;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * 实体类初始化配置，确保关键实体类在Spring Context启动时被加载
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EntityInitializationConfig {

    /**
     * 确保UserToken类在启动时被加载
     */
    @Bean
    @Primary
    public Class<UserToken> userTokenClass() {
        return UserToken.class;
    }
} 