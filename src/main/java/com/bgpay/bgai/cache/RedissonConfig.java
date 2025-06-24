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

    @Value("${bgpay.bgai.redis.single.connectionPoolSize:16}")
    private int connectionPoolSize;

    @Value("${bgpay.bgai.redis.single.connectionMinimumIdleSize:4}")
    private int connectionMinimumIdleSize;

    @Value("${bgpay.bgai.redis.single.idleConnectionTimeout:10000}")
    private int idleConnectionTimeout;

    @Value("${bgpay.bgai.redis.single.connectTimeout:10000}")
    private int connectTimeout;

    @Value("${bgpay.bgai.redis.single.timeout:3000}")
    private int timeout;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(redisSingleAddress)
                .setPassword(redisSinglePassword)
                .setConnectionPoolSize(connectionPoolSize)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setIdleConnectionTimeout(idleConnectionTimeout)
                .setConnectTimeout(connectTimeout)
                .setTimeout(timeout);
        return Redisson.create(config);
    }
}