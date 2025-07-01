package com.bgpay.bgai.feign;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import com.bgpay.bgai.feign.fallback.UserServiceFallbackFactory;

/**
 * 用户服务Feign客户端
 * 用于调用远程用户服务
 */
@FeignClient(
    name = "user-service", 
    url = "${bgai.feign.user-service.url:http://localhost:8688}",
    fallbackFactory = UserServiceFallbackFactory.class,
    configuration = UserServiceFeignConfig.class,
    primary = true
)
@ConditionalOnProperty(name = "bgai.feign.enabled", havingValue = "true", matchIfMissing = false)
public interface UserServiceClient {

    /**
     * 获取所有用户
     */
    @GetMapping("/api/users")
    List<Map<String, Object>> listUsers();

    /**
     * 根据ID获取用户
     */
    @GetMapping("/api/users/{id}")
    Map<String, Object> getUserById(@PathVariable("id") String id);

    /**
     * 创建用户
     */
    @PostMapping("/api/users")
    Map<String, Object> createUser(@RequestBody Map<String, Object> userData);

    /**
     * 更新用户
     */
    @PutMapping("/api/users/{id}")
    Map<String, Object> updateUser(@PathVariable("id") String id, @RequestBody Map<String, Object> userData);

    /**
     * 删除用户
     */
    @DeleteMapping("/api/users/{id}")
    Map<String, Object> deleteUser(@PathVariable("id") String id);
    
    /**
     * 健康检查
     */
    @GetMapping("/api/users/health")
    Map<String, Object> health();
    
    /**
     * 测试错误场景
     */
    @GetMapping("/api/users/error")
    Map<String, Object> testError();

    /**
     * 测试超时场景
     */
    @GetMapping("/api/users/timeout")
    Map<String, Object> testTimeout();

    /**
     * 测试超时场景 - 可配置超时时间
     */
    @GetMapping("/api/users/timeout")
    Map<String, Object> testTimeoutWithParam(@RequestParam("duration") Long duration);
} 