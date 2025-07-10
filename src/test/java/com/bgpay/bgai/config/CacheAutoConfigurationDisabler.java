package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to disable Cache auto-configuration in test environment
 */
@Configuration
@EnableAutoConfiguration(exclude = {
        CacheAutoConfiguration.class
})
public class CacheAutoConfigurationDisabler {
    // This class intentionally left empty
    // Its purpose is to disable Cache auto-configuration
} 