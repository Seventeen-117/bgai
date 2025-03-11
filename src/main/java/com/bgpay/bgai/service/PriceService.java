package com.bgpay.bgai.service;

import com.bgpay.bgai.entity.PriceConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Component
public class PriceService {
    private final RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private PriceConfigService priceConfigService;

    private static final String PRICE_KEY = "price:%s:%s:%s";

    // 初始化价格配置到Redis
    @PostConstruct
    public void initPriceCache() {
        List<PriceConfig> latestConfigs = priceConfigService.getPriceConfigsByVersion(1);
        if (latestConfigs != null) {
            latestConfigs.forEach(config -> {
                String key = generateCacheKey(config);
                redisTemplate.opsForValue().set(key, config.getPrice());
            });
        }
    }

    // 获取价格
    public BigDecimal getPrice(String modelType, String timeType, String priceType) throws Exception {
        String key = generateCacheKey(modelType, timeType, priceType);
        BigDecimal price = (BigDecimal) redisTemplate.opsForValue().get(key);
        if (price == null) {
            throw new Exception("价格配置不存在");
        }
        return price;
    }

    @Scheduled(cron = "0 0/30 * * * ?") // 每30分钟刷新一次
    public void refreshPriceCache() {
        List<PriceConfig> latestConfigs = priceConfigService.getPriceConfigsByVersion(1);
        if (latestConfigs != null) {
            latestConfigs.forEach(config -> {
                String key = generateCacheKey(config);
                redisTemplate.opsForValue().set(key, config.getPrice(), 1, TimeUnit.HOURS);
            });
        }
    }

    // 生成缓存键的方法
    private String generateCacheKey(PriceConfig config) {
        return String.format(PRICE_KEY,
                config.getModelType(),
                config.getTimePeriod(),
                config.getIoType());
    }

    // 重载生成缓存键的方法，用于根据参数生成缓存键
    private String generateCacheKey(String modelType, String timeType, String priceType) {
        return String.format(PRICE_KEY, modelType, timeType, priceType);
    }
}