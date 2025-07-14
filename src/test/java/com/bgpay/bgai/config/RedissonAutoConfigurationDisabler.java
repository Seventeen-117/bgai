package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to disable Redisson auto-configuration in test environment
 */
@Configuration
@EnableAutoConfiguration(exclude = {
        org.redisson.spring.starter.RedissonAutoConfiguration.class
})
public class RedissonAutoConfigurationDisabler {

} 