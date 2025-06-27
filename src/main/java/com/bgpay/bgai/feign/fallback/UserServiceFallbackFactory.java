package com.bgpay.bgai.feign.fallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import com.bgpay.bgai.feign.LocalUserServiceClient;
import com.bgpay.bgai.feign.UserServiceClient;

import lombok.extern.slf4j.Slf4j;

/**
 * 用户服务降级工厂
 * 当用户服务不可用时提供降级实现
 */
@Slf4j
@Component
public class UserServiceFallbackFactory implements FallbackFactory<UserServiceClient> {

    @Override
    public UserServiceClient create(Throwable cause) {
        // 提取根本异常以得到更准确的错误信息
        Throwable rootCause = extractRootCause(cause);
        String errorMessage = rootCause.getMessage();
        log.warn("用户服务调用失败，回退处理。原因: {}", errorMessage);
        
        return new UserServiceClient() {
            @Override
            public List<Map<String, Object>> listUsers() {
                log.debug("获取用户列表失败，回退处理");
                List<Map<String, Object>> fallbackList = new ArrayList<>();
                Map<String, Object> fallbackUser = new HashMap<>();
                fallbackUser.put("id", "-1");
                fallbackUser.put("username", "fallback-user");
                fallbackUser.put("name", "Fallback User");
                fallbackUser.put("email", "fallback@example.com");
                fallbackUser.put("_fallback", true);
                fallbackUser.put("_error", errorMessage);
                fallbackList.add(fallbackUser);
                return fallbackList;
            }

            @Override
            public Map<String, Object> getUserById(String id) {
                log.debug("获取用户详情失败，回退处理。ID: {}", id);
                Map<String, Object> fallbackUser = new HashMap<>();
                fallbackUser.put("id", id);
                fallbackUser.put("username", "fallback-user-" + id);
                fallbackUser.put("name", "Fallback User " + id);
                fallbackUser.put("email", "fallback-" + id + "@example.com");
                fallbackUser.put("_fallback", true);
                fallbackUser.put("_error", errorMessage);
                return fallbackUser;
            }

            @Override
            public Map<String, Object> createUser(Map<String, Object> userData) {
                log.debug("创建用户失败，回退处理");
                Map<String, Object> fallbackResult = new HashMap<>(userData);
                fallbackResult.put("id", "-1");
                fallbackResult.put("_fallback", true);
                fallbackResult.put("_error", errorMessage);
                fallbackResult.put("message", "用户创建请求已接收，但服务暂时不可用");
                return fallbackResult;
            }

            @Override
            public Map<String, Object> updateUser(String id, Map<String, Object> userData) {
                log.debug("更新用户失败，回退处理。ID: {}", id);
                Map<String, Object> fallbackResult = new HashMap<>(userData);
                fallbackResult.put("id", id);
                fallbackResult.put("_fallback", true);
                fallbackResult.put("_error", errorMessage);
                fallbackResult.put("message", "用户更新请求已接收，但服务暂时不可用");
                return fallbackResult;
            }

            @Override
            public Map<String, Object> deleteUser(String id) {
                log.debug("删除用户失败，回退处理。ID: {}", id);
                Map<String, Object> fallbackResult = new HashMap<>();
                fallbackResult.put("id", id);
                fallbackResult.put("_fallback", true);
                fallbackResult.put("_error", errorMessage);
                fallbackResult.put("message", "用户删除请求已接收，但服务暂时不可用");
                return fallbackResult;
            }
            
            @Override
            public Map<String, Object> health() {
                log.debug("健康检查失败，回退处理");
                Map<String, Object> fallbackHealth = new HashMap<>();
                fallbackHealth.put("status", "DOWN");
                fallbackHealth.put("service", "user-service-fallback");
                fallbackHealth.put("error", errorMessage);
                fallbackHealth.put("_fallback", true);
                return fallbackHealth;
            }
            
            @Override
            public Map<String, Object> testError() {
                log.debug("错误测试成功触发降级逻辑");
                Map<String, Object> fallbackResult = new HashMap<>();
                fallbackResult.put("status", "error_fallback");
                fallbackResult.put("error", errorMessage);
                fallbackResult.put("_fallback", true);
                fallbackResult.put("timestamp", System.currentTimeMillis());
                fallbackResult.put("message", "这是一个错误测试的降级响应");
                return fallbackResult;
            }
            
            @Override
            public Map<String, Object> testTimeout() {
                log.debug("超时测试失败，回退处理");
                Map<String, Object> fallbackResult = new HashMap<>();
                fallbackResult.put("status", "timeout_fallback");
                fallbackResult.put("error", errorMessage);
                fallbackResult.put("_fallback", true);
                fallbackResult.put("timestamp", System.currentTimeMillis());
                fallbackResult.put("message", "这是一个超时测试的降级响应");
                return fallbackResult;
            }
            
            @Override
            public Map<String, Object> testTimeoutWithParam(Long duration) {
                log.debug("带参数的超时测试失败，回退处理。参数: {} ms", duration);
                Map<String, Object> fallbackResult = new HashMap<>();
                fallbackResult.put("status", "timeout_param_fallback");
                fallbackResult.put("error", errorMessage);
                fallbackResult.put("duration", duration);
                fallbackResult.put("_fallback", true);
                fallbackResult.put("timestamp", System.currentTimeMillis());
                fallbackResult.put("message", "这是一个带参数的超时测试降级响应");
                return fallbackResult;
            }
        };
    }
    
    /**
     * 提取异常链中最原始的异常
     * @param throwable 异常
     * @return 根本原因异常
     */
    private Throwable extractRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
} 