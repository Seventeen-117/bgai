package com.bgpay.bgai.config;

import com.bgpay.bgai.cache.CacheWarmer;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 为测试提供CacheWarmer的Mock实现
 */
@Configuration
public class CacheWarmerMockConfig {

    /**
     * 提供CacheWarmer的Mock实现
     */
    @Bean
    @Primary
    public CacheWarmer cacheWarmer() {
        return Mockito.mock(CacheWarmer.class);
    }
} 