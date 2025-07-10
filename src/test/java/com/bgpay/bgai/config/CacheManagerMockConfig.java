package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheManager;

/**
 * Mock configuration for CacheManager in test environment
 */
@Configuration
public class CacheManagerMockConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        return Mockito.mock(RedisCacheManager.class);
    }
} 