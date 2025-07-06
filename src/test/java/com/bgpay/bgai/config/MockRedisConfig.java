package com.bgpay.bgai.config;

import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 测试环境使用的模拟Redis配置
 * 使用内存Map代替实际的Redis进行测试
 */
@Configuration
@ConditionalOnProperty(name = "spring.redis.mock", havingValue = "true", matchIfMissing = true)
public class MockRedisConfig {
    private static final Logger log = LoggerFactory.getLogger(MockRedisConfig.class);
    
    // 内存缓存用于模拟Redis
    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    
    /**
     * 提供模拟的ReactiveRedisTemplate
     */
    @Bean
    @Primary
    public <K, V> ReactiveRedisTemplate<K, V> reactiveRedisTemplate() {
        log.info("创建模拟ReactiveRedisTemplate");
        
        @SuppressWarnings("unchecked")
        ReactiveRedisTemplate<K, V> mockTemplate = Mockito.mock(ReactiveRedisTemplate.class);
        ReactiveValueOperations<K, V> mockValueOps = createMockValueOps();
        
        Mockito.when(mockTemplate.opsForValue()).thenReturn(mockValueOps);
        
        return mockTemplate;
    }
    
    /**
     * 提供用于字符串操作的专用模板
     */
    @Bean
    @Primary
    public ReactiveRedisTemplate<String, String> reactiveStringRedisTemplate() {
        log.info("创建模拟ReactiveStringRedisTemplate");
        
        @SuppressWarnings("unchecked")
        ReactiveRedisTemplate<String, String> mockTemplate = Mockito.mock(ReactiveRedisTemplate.class);
        ReactiveValueOperations<String, String> mockValueOps = createMockValueOps();
        
        Mockito.when(mockTemplate.opsForValue()).thenReturn(mockValueOps);
        
        return mockTemplate;
    }
    
    /**
     * 提供模拟的ReactiveRedisConnectionFactory
     */
    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        log.info("创建模拟ReactiveRedisConnectionFactory");
        return Mockito.mock(ReactiveRedisConnectionFactory.class);
    }
    
    /**
     * 创建模拟的ReactiveValueOperations
     */
    @SuppressWarnings("unchecked")
    private <K, V> ReactiveValueOperations<K, V> createMockValueOps() {
        ReactiveValueOperations<K, V> mockValueOps = Mockito.mock(ReactiveValueOperations.class);
        
        // 实现get操作
        Mockito.when(mockValueOps.get(Mockito.any())).thenAnswer(invocation -> {
            K key = invocation.getArgument(0);
            V value = (V) cache.get(key.toString());
            log.debug("Mock Redis GET: key={}, value={}", key, value);
            return value != null ? reactor.core.publisher.Mono.just(value) : reactor.core.publisher.Mono.empty();
        });
        
        // 实现set操作
        Mockito.when(mockValueOps.set(Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
            K key = invocation.getArgument(0);
            V value = invocation.getArgument(1);
            log.debug("Mock Redis SET: key={}, value={}", key, value);
            cache.put(key.toString(), value);
            return reactor.core.publisher.Mono.just(Boolean.TRUE);
        });
        
        // 实现带过期时间的set操作
        Mockito.when(mockValueOps.set(Mockito.any(), Mockito.any(), Mockito.any(Duration.class)))
                .thenAnswer(invocation -> {
                    K key = invocation.getArgument(0);
                    V value = invocation.getArgument(1);
                    Duration ttl = invocation.getArgument(2);
                    log.debug("Mock Redis SET with TTL: key={}, value={}, ttl={}", key, value, ttl);
                    cache.put(key.toString(), value);
                    return reactor.core.publisher.Mono.just(Boolean.TRUE);
                });
        
        // 实现delete操作
        Mockito.when(mockValueOps.delete(Mockito.any())).thenAnswer(invocation -> {
            K key = invocation.getArgument(0);
            log.debug("Mock Redis DELETE: key={}", key);
            V value = (V) cache.remove(key.toString());
            return reactor.core.publisher.Mono.just(value != null);
        });
        
        return mockValueOps;
    }
} 