package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

/**
 * Mock configuration for Redisson client in test environment
 */
@Configuration
@TestPropertySource(properties = "spring.redis.enabled=false")
public class RedissonMockConfig {

    @Bean
    @Primary
    public RedissonClient redissonClient() {
        // Create a mock RedissonClient
        RedissonClient mockRedissonClient = Mockito.mock(RedissonClient.class);
        
        // Create a mock RLock
        RLock mockLock = Mockito.mock(RLock.class);
        
        // Configure the mock RLock behavior
        Mockito.when(mockLock.isHeldByCurrentThread()).thenReturn(true);
        Mockito.doNothing().when(mockLock).unlock();
        
        try {
            // Configure tryLock to return true
            Mockito.when(mockLock.tryLock(Mockito.anyLong(), Mockito.anyLong(), Mockito.any())).thenReturn(true);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        // Configure the mock RedissonClient to return our mock lock
        Mockito.when(mockRedissonClient.getLock(Mockito.anyString())).thenReturn(mockLock);
        
        return mockRedissonClient;
    }
} 