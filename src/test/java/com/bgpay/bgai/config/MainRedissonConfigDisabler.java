package com.bgpay.bgai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration class to disable the main application's RedissonConfig
 */
@Configuration
@Import(RedissonMockConfig.class)
public class MainRedissonConfigDisabler {
    // This class intentionally left empty
    // Its purpose is to disable the main application's RedissonConfig
} 