package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Filter to exclude Redisson's auto-configuration in test environment
 */
public class RedissonAutoConfigurationExclusionFilter implements AutoConfigurationImportFilter {

    private static final Set<String> EXCLUDED_AUTO_CONFIGURATIONS = new HashSet<>(Arrays.asList(
            "org.redisson.spring.starter.RedissonAutoConfiguration",
            "com.bgpay.bgai.cache.RedissonConfig"
    ));

    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean[] matches = new boolean[autoConfigurationClasses.length];
        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            matches[i] = !EXCLUDED_AUTO_CONFIGURATIONS.contains(autoConfigurationClasses[i]);
        }
        return matches;
    }
} 