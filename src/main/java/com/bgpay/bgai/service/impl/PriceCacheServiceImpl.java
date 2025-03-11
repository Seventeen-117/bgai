package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.entity.PriceConfig;
import com.bgpay.bgai.entity.PriceQuery;
import com.bgpay.bgai.exception.BillingException;
import com.bgpay.bgai.service.PriceCacheService;
import com.bgpay.bgai.service.PriceConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * This service class is responsible for caching price configurations in Redis.
 * It uses Redisson for distributed locking to ensure thread - safety when accessing the cache.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PriceCacheServiceImpl implements PriceCacheService {
    // Redis template for interacting with Redis
    private final RedisTemplate<String, Object> redisTemplate;
    // Service for retrieving price configurations from the database
    private final PriceConfigService priceConfigService;
    // Redisson client for distributed locking
    private final RedissonClient redissonClient;

    // Cache time - to - live, set to 1 hour
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    // Prefix for null cache keys, used to cache the absence of a price configuration
    private static final String NULL_CACHE_PREFIX = "NULL:";
    // Random number generator for adding randomness to cache expiration times
    private static final Random RANDOM = new Random();

    /**
     * Retrieves a price configuration from the cache. If the configuration is not in the cache,
     * it tries to fetch it from the database and then caches it.
     *
     * @param query The price query object containing conditions for retrieving the price configuration.
     * @return The price configuration if found, otherwise null.
     */
    @Override
    @Cacheable(value = "priceConfigs", keyGenerator = "priceKeyGenerator",
            unless = "#result == null")
    public PriceConfig getPriceConfig(PriceQuery query) {
        // Generate the cache key based on the query
        String cacheKey = generateCacheKey(query);
        // Generate the null cache key
        String nullKey = NULL_CACHE_PREFIX + cacheKey;
        // Get a distributed lock for the cache key
        RLock lock = redissonClient.getLock(cacheKey + ":lock");

        try {
            // Try to acquire the lock with a 100ms wait time and a 30 - second lock - hold time
            if (lock.tryLock(100, 30000, TimeUnit.MILLISECONDS)) {
                // Try to get the price configuration from the cache
                PriceConfig config = (PriceConfig) redisTemplate.opsForValue().get(cacheKey);
                if (config != null) {
                    return config;
                }

                // Check if the null cache key exists
                if (Boolean.TRUE.equals(redisTemplate.hasKey(nullKey))) {
                    return null;
                }

                // Try to get the price configuration from the database
                config = priceConfigService.findValidPriceConfig(query);
                if (config == null) {
                    // Cache the absence of a price configuration with a random expiration time between 5 and 15 minutes
                    redisTemplate.opsForValue().set(nullKey, "",
                            Duration.ofMinutes(5 + RANDOM.nextInt(10)));
                    return null;
                }

                // Cache the price configuration with a random expiration time within 300 seconds of the base TTL
                redisTemplate.opsForValue().set(cacheKey, config,
                        CACHE_TTL.plusSeconds(RANDOM.nextInt(300)));
                return config;
            }
            // Return null if the lock cannot be acquired, and let the upper - layer handle it
            return null;
        } catch (InterruptedException e) {
            // Interrupt the current thread and throw a BillingException
            Thread.currentThread().interrupt();
            throw new BillingException("Interrupted while getting price configuration", e);
        } finally {
            // Release the lock if it is held by the current thread
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * Refreshes the entire price cache by evicting all entries.
     */
    @Override
    @CacheEvict(value = "priceConfigs", allEntries = true)
    public void refreshCache() {
        // Log the cache refresh time
        log.info("Full price cache refreshed at {}", LocalDateTime.now());
    }

    /**
     * Evicts a specific cache entry based on the given price query.
     *
     * @param query The price query object used to generate the cache key to be evicted.
     */
    @Override
    public void evictCacheByQuery(PriceQuery query) {
        // Generate the cache key based on the query
        String key = generateCacheKey(query);
        // Delete the cache entry
        redisTemplate.delete(key);
        // Log the eviction of the cache entry
        log.debug("Evict cache for key: {}", key);
    }

    /**
     * Generates a cache key based on the given price query.
     *
     * @param query The price query object containing information for generating the cache key.
     * @return The generated cache key.
     */
    private String generateCacheKey(PriceQuery query) {
        return String.format("price:%s:%s:%s:%s",
                query.getModelType(),
                query.getTimePeriod(),
                query.getCacheStatus(),
                query.getIoType());
    }

    /**
     * Scans Redis keys that match the given pattern.
     * In a production environment, using the SCAN command is recommended.
     *
     * @param pattern The pattern used to match Redis keys.
     * @return A set of keys that match the pattern.
     */
    private Set<String> scanRedisKeys(String pattern) {
        return redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            // Create a set to store the matching keys
            Set<String> keys = new HashSet<>();
            // Use the SCAN command to iterate over keys that match the pattern
            Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions()
                    .match(pattern)
                    .count(100)
                    .build());
            while (cursor.hasNext()) {
                // Add the matching key to the set
                keys.add(new String(cursor.next()));
            }
            return keys;
        });
    }

    /**
     * Parses a price query from a cache key.
     *
     * @param key The cache key to be parsed.
     * @return The parsed price query object.
     */
    private PriceQuery parseQueryFromKey(String key) {
        // Split the cache key by colon
        String[] parts = key.split(":");
        return new PriceQuery(parts[1], parts[2], parts[3], parts[4]);
    }

    /**
     * Refreshes the cache for a specific model by deleting all cache entries related to that model.
     *
     * @param modelType The model type for which the cache needs to be refreshed.
     */
    public void refreshCacheByModel(String modelType) {
        // Generate the pattern for matching cache keys related to the model
        String pattern = "price:" + modelType + ":*";
        // Scan for keys that match the pattern
        Set<String> keys = scanRedisKeys(pattern);
        if (!keys.isEmpty()) {
            // Delete all matching keys from the cache
            redisTemplate.delete(keys);
        }
        // Log the cache refresh for the model
        log.info("Refreshed cache for model: {}", modelType);
    }
}