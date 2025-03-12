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
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
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

    private final RedisTemplate<String, PriceConfig> priceConfigTemplate;
    private final RedisTemplate<String, String> stringTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PriceConfigService priceConfigService;
    private final RedissonClient redissonClient;
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final String NULL_CACHE_PREFIX = "NULL:";
    private static final Random RANDOM = new Random();

    /**
     * Retrieves a price configuration from the cache. If the configuration is not in the cache,
     * it tries to fetch it from the database and then caches it.
     *
     * @param query The price query object containing conditions for retrieving the price configuration.
     * @return The price configuration if found, otherwise null.
     */
    @Override
    public PriceConfig getPriceConfig(PriceQuery query) {
        String cacheKey = generateCacheKey(query);
        String nullKey = NULL_CACHE_PREFIX + cacheKey;
        RLock lock = redissonClient.getLock(cacheKey + ":lock");

        try {
            if (lock.tryLock(100, 30000, TimeUnit.MILLISECONDS)) {
                // 增加异常捕获逻辑
                try {
                    PriceConfig config = priceConfigTemplate.opsForValue().get(cacheKey);
                    if (config != null) return config;
                } catch (Exception e) {
                    log.warn("反序列化失败，删除无效缓存: {}", cacheKey, e);
                    priceConfigTemplate.delete(cacheKey); // 清理无效缓存
                }

                if (Boolean.TRUE.equals(stringTemplate.hasKey(nullKey))) {
                    return null;
                }

                PriceConfig config = priceConfigService.findValidPriceConfig(query);
                if (config == null) {
                    stringTemplate.opsForValue().set(nullKey, "empty", Duration.ofMinutes(5 + RANDOM.nextInt(10)));
                    return null;
                }

                priceConfigTemplate.opsForValue().set(cacheKey, config, CACHE_TTL.plusSeconds(RANDOM.nextInt(300)));
                return config;
            }
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BillingException("Interrupted while getting price configuration", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    /**
     * Generates a cache key based on the given price query.
     *
     * @param query The price query object containing information for generating the cache key.
     * @return The generated cache key.
     */
    private String generateCacheKey(PriceQuery query) {
        // Generate a cache key using the model type, time period, cache status, and IO type from the query
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
            // Configure the scan options with the given pattern and a count of 100 keys per scan
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(100)
                    .build();

            // Use a cursor to iterate over the keys that match the pattern
            Cursor<byte[]> cursor = connection.scan(options);
            while (cursor.hasNext()) {
                // Convert the byte array key to a string and add it to the set
                keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
            return keys;
        });
    }

    /**
     * Clears the price configuration cache, including both manually stored caches and those generated by @Cacheable annotations.
     */
    @Override
    public void clearPriceConfigCache() {
        // Scan for manually stored cache keys that match the "price:*" pattern
        Set<String> manualKeys = scanRedisKeys("price:*");
        if (!manualKeys.isEmpty()) {
            // Delete all the manually stored cache keys
            redisTemplate.delete(manualKeys);
            // Log the number of manually deleted cache keys
            log.info("Deleted {} manual cache keys", manualKeys.size());
        }

        // Scan for cache keys generated by @Cacheable annotations that match the "priceConfigs:*" pattern
        Set<String> annotationKeys = scanRedisKeys("priceConfigs:*");
        if (!annotationKeys.isEmpty()) {
            // Delete all the cache keys generated by @Cacheable annotations
            redisTemplate.delete(annotationKeys);
            // Log the number of annotation - generated cache keys deleted
            log.info("Deleted {} annotation cache keys", annotationKeys.size());
        }
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
        // Create a new PriceQuery object using the parts of the split key
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