package com.bgpay.bgai.feign;

import feign.Feign;
import feign.Logger;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.openfeign.CircuitBreakerNameResolver;
import org.springframework.cloud.openfeign.FeignCircuitBreaker;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignFormatterRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.time.Duration;

/**
 * Feign配置类
 * 整合Resilience4j和LoadBalancer
 */
@Configuration
@EnableFeignClients(basePackages = "com.bgpay.bgai.feign")
@ConditionalOnProperty(name = "bgai.feign.enabled", havingValue = "true", matchIfMissing = false)
public class FeignConfig {

    /**
     * 配置Feign日志级别
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * 自定义Circuit Breaker命名解析器
     * 使Feign客户端与Resilience4J的熔断器可以关联
     */
    @Bean
    public CircuitBreakerNameResolver circuitBreakerNameResolver() {
        return (feignClientName, target, method) ->
                String.format("%s_%s_%s", feignClientName, target.name(), method.getName());
    }

    /**
     * 为Feign配置日期格式化处理器
     */
    @Bean
    public FeignFormatterRegistrar dateTimeFormatterRegistrar() {
        return registry -> {
            DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
            registrar.setUseIsoFormat(true);
            registrar.registerFormatters(registry);
        };
    }

    /**
     * 自定义Feign Builder，集成Resilience4j熔断器
     * 这里重用了已有的ResilienceConfig配置
     */
    @Bean
    @Primary
    public Feign.Builder feignBuilder() {
        return FeignCircuitBreaker.builder();
    }

    /**
     * 为特定Feign客户端配置熔断器
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> feignClientCustomizer() {
        return factory -> {
            factory.configure(builder -> builder
                    .circuitBreakerConfig(CircuitBreakerConfig.custom()
                            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                            .slidingWindowSize(10)
                            .failureRateThreshold(50.0f)
                            .waitDurationInOpenState(Duration.ofSeconds(10))
                            .permittedNumberOfCallsInHalfOpenState(5)
                            .build())
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(10))
                            .build()), "feignClientDefault");
        };
    }

    /**
     * 提供HttpMessageConverters bean
     * 这是Feign客户端解码响应所必需的
     */
    @Bean
    @Primary
    public HttpMessageConverters httpMessageConverters() {
        HttpMessageConverter<?> jacksonConverter = new MappingJackson2HttpMessageConverter();
        return new HttpMessageConverters(jacksonConverter);
    }
    
    /**
     * 长超时配置
     */
    @Bean(name = "longTimeoutOptions")
    public feign.Request.Options longTimeoutOptions() {
        return new feign.Request.Options(20000, 20000);
    }
} 