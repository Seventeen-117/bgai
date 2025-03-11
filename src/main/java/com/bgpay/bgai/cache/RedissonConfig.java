package com.bgpay.bgai.cache;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${bgpay.bgai.redis.single.address}")
    private String redisSingleAddress;

    @Value("${bgpay.bgai.redis.single.password}")
    private String redisSinglePassword;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(redisSingleAddress)
                .setPassword(redisSinglePassword);
        return Redisson.create(config);
    }
}