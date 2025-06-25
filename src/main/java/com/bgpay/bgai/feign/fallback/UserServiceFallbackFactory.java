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
        log.error("用户服务调用失败，回退处理。原因: {}", cause.getMessage());
        
        return new UserServiceClient() {
            @Override
            public List<Map<String, Object>> listUsers() {
                log.error("获取用户列表失败，回退处理。原因: {}", cause);
                List<Map<String, Object>> fallbackList = new ArrayList<>();
                Map<String, Object> fallbackUser = new HashMap<>();
                fallbackUser.put("id", "-1");
                fallbackUser.put("username", "fallback-user");
                fallbackUser.put("name", "Fallback User");
                fallbackUser.put("email", "fallback@example.com");
                fallbackUser.put("_fallback", true);
                fallbackUser.put("_error", cause.getMessage());
                fallbackList.add(fallbackUser);
                return fallbackList;
            }

            @Override
            public Map<String, Object> getUserById(String id) {
                log.error("获取用户详情失败，回退处理。ID: {}, 原因: {}", id, cause);
                Map<String, Object> fallbackUser = new HashMap<>();
                fallbackUser.put("id", id);
                fallbackUser.put("username", "fallback-user-" + id);
                fallbackUser.put("name", "Fallback User " + id);
                fallbackUser.put("email", "fallback-" + id + "@example.com");
                fallbackUser.put("_fallback", true);
                fallbackUser.put("_error", cause.getMessage());
                return fallbackUser;
            }

            @Override
            public Map<String, Object> createUser(Map<String, Object> userData) {
                log.error("创建用户失败，回退处理。用户数据: {}, 原因: {}", userData, cause);
                Map<String, Object> fallbackResult = new HashMap<>(userData);
                fallbackResult.put("id", "-1");
                fallbackResult.put("_fallback", true);
                fallbackResult.put("_error", cause.getMessage());
                fallbackResult.put("message", "用户创建请求已接收，但服务暂时不可用");
                return fallbackResult;
            }

            @Override
            public Map<String, Object> updateUser(String id, Map<String, Object> userData) {
                log.error("更新用户失败，回退处理。ID: {}, 用户数据: {}, 原因: {}", id, userData, cause);
                Map<String, Object> fallbackResult = new HashMap<>(userData);
                fallbackResult.put("id", id);
                fallbackResult.put("_fallback", true);
                fallbackResult.put("_error", cause.getMessage());
                fallbackResult.put("message", "用户更新请求已接收，但服务暂时不可用");
                return fallbackResult;
            }

            @Override
            public Map<String, Object> deleteUser(String id) {
                log.error("删除用户失败，回退处理。ID: {}, 原因: {}", id, cause);
                Map<String, Object> fallbackResult = new HashMap<>();
                fallbackResult.put("id", id);
                fallbackResult.put("_fallback", true);
                fallbackResult.put("_error", cause.getMessage());
                fallbackResult.put("message", "用户删除请求已接收，但服务暂时不可用");
                return fallbackResult;
            }
            
            @Override
            public Map<String, Object> health() {
                log.error("健康检查失败，回退处理。原因: {}", cause);
                Map<String, Object> fallbackHealth = new HashMap<>();
                fallbackHealth.put("status", "DOWN");
                fallbackHealth.put("service", "user-service-fallback");
                fallbackHealth.put("error", cause.getMessage());
                fallbackHealth.put("_fallback", true);
                return fallbackHealth;
            }
            
            @Override
            public Map<String, Object> testError() {
                log.error("错误测试失败，回退处理。原因: {}", cause);
                Map<String, Object> fallbackResult = new HashMap<>();
                fallbackResult.put("status", "error_fallback");
                fallbackResult.put("error", cause.getMessage());
                fallbackResult.put("_fallback", true);
                return fallbackResult;
            }
            
            @Override
            public Map<String, Object> testTimeout() {
                log.error("超时测试失败，回退处理。原因: {}", cause);
                Map<String, Object> fallbackResult = new HashMap<>();
                fallbackResult.put("status", "timeout_fallback");
                fallbackResult.put("error", cause.getMessage());
                fallbackResult.put("_fallback", true);
                return fallbackResult;
            }
        };
    }
} 