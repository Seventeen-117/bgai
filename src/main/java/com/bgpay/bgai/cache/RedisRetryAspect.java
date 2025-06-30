package com.bgpay.bgai.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis操作重试切面
 * 为使用RedisTemplate的方法添加重试机制
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisRetryAspect {

    private final RetryTemplate redisRetryTemplate;

    /**
     * 对所有RedisTemplate方法调用添加重试机制
     * @param joinPoint 切点
     * @return 方法执行结果
     * @throws Throwable 如果重试后仍然失败
     */
    @Around("execution(* org.springframework.data.redis.core.RedisTemplate+.*(..))" +
            " || execution(* org.springframework.data.redis.core.StringRedisTemplate+.*(..))" +
            " || execution(* org.springframework.data.redis.core.ValueOperations+.*(..))" +
            " || execution(* org.springframework.data.redis.core.HashOperations+.*(..))")
    public Object retryAroundRedisOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        try {
            return redisRetryTemplate.execute(context -> {
                try {
                    int retryCount = context.getRetryCount();
                    if (retryCount > 0) {
                        log.info("Redis操作重试 - 类: {}, 方法: {}, 重试次数: {}", className, methodName, retryCount);
                    }
                    return joinPoint.proceed();
                } catch (Throwable e) {
                    if (e instanceof QueryTimeoutException || e instanceof RedisConnectionFailureException) {
                        log.warn("Redis操作失败，准备重试 - 类: {}, 方法: {}, 异常: {}", 
                                className, methodName, e.getMessage());
                        throw (RuntimeException) e;
                    } else {
                        throw new RuntimeException("Redis操作失败", e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("Redis操作在多次重试后失败 - 类: {}, 方法: {}, 异常: {}", 
                    className, methodName, e.getMessage(), e);
            throw e;
        }
    }
} 