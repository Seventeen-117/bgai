package com.bgpay.bgai.controller;

import com.bgpay.bgai.entity.User;
import com.bgpay.bgai.feign.LocalUserServiceClient;
import com.bgpay.bgai.feign.UserServiceClient;
import feign.Request;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Feign示例控制器
 * 用于演示OpenFeign的使用
 * 通过配置属性bgai.feign.demo.enabled可以控制是否启用该控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/feign-demo")
@ConditionalOnProperty(name = "bgai.feign.demo.enabled", havingValue = "true", matchIfMissing = true)
public class FeignDemoController {

    private final UserServiceClient userServiceClient;
    private final LocalUserServiceClient localUserServiceClient;
    
    @Value("${bgai.api-key.test-key:test-api-key-123}")
    private String testApiKey;
    
    @Autowired(required = false)
    @Qualifier("longTimeoutOptions")
    private Request.Options longTimeoutOptions;

    @Autowired
    public FeignDemoController(UserServiceClient userServiceClient, 
                              LocalUserServiceClient localUserServiceClient) {
        this.userServiceClient = userServiceClient;
        this.localUserServiceClient = localUserServiceClient;
        log.info("FeignDemoController initialized with UserServiceClient and LocalUserServiceClient");
    }

    /**
     * 获取所有用户
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        log.info("Fetching all users using Feign client");
        try {
            return ResponseEntity.ok(userServiceClient.listUsers());
        } catch (Exception e) {
            log.error("Error fetching users: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch users");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 获取本地用户
     */
    @GetMapping("/local/users")
    public ResponseEntity<?> getLocalUsers() {
        log.info("Fetching users from local service");
        try {
            return ResponseEntity.ok(localUserServiceClient.listUsers());
        } catch (Exception e) {
            log.error("Error fetching local users: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch local users");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id) {
        log.info("Fetching user with ID: {}", id);
        try {
            return ResponseEntity.ok(userServiceClient.getUserById(id));
        } catch (Exception e) {
            log.error("Error fetching user {}: {}", id, e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch user");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 创建用户
     */
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> userData) {
        log.info("Creating user: {}", userData);
        try {
            return ResponseEntity.ok(userServiceClient.createUser(userData));
        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create user");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 测试超时 - 改进版本，使用Mono和自定义超时处理
     */
    @GetMapping("/timeout-test")
    public Object testTimeout() {
        log.info("Testing timeout scenario with improved handling");
        
        try {
            // 使用CompletableFuture设置更短的超时
            CompletableFuture<Map<String, Object>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // 这个调用应该会超时
                    return userServiceClient.testTimeout();
                } catch (Exception e) {
                    log.error("Inner timeout exception: {}", e.getMessage());
                    throw new RuntimeException(e);
                }
            });
            
            // 等待结果，但只等12秒（比服务端的15秒短）
            Map<String, Object> result = future.get(12, TimeUnit.SECONDS);
            return ResponseEntity.ok(result);
        } catch (TimeoutException e) {
            log.warn("Timeout occurred as expected: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "timeout_handled_successfully");
            response.put("message", "超时测试成功：请求在12秒后超时");
            response.put("errorType", e.getClass().getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Timeout test triggered exception: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "timeout_error");
            response.put("message", e.getMessage());
            response.put("errorType", e.getClass().getName());
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 测试超时 - 另一种方式
     * 使用响应式方式处理超时
     */
    @GetMapping("/timeout-test-alt")
    public Mono<ResponseEntity<Object>> testTimeoutAlt() {
        log.info("Testing timeout scenario with reactive approach");
        
        return Mono.fromCallable(() -> {
            try {
                return userServiceClient.testTimeout();
            } catch (Exception e) {
                log.error("Timeout exception in reactive flow: {}", e.getMessage());
                throw e;
            }
        })
        .map(result -> ResponseEntity.ok().body((Object)result))
        .timeout(java.time.Duration.ofSeconds(12))
        .onErrorResume(e -> {
            log.warn("Reactive timeout handled: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "reactive_timeout_handled");
            response.put("message", "响应式超时处理成功");
            response.put("errorType", e.getClass().getName());
            return Mono.just(ResponseEntity.ok().body((Object)response));
        })
        .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 测试错误处理 - 改用对象返回
     */
    @GetMapping("/error-test")
    public ResponseEntity<?> testError() {
        log.info("Testing error scenario with non-reactive approach");
        
        try {
            // 这个调用应该会返回错误或降级响应
            Map<String, Object> result = userServiceClient.testError();
            
            // 检查结果中是否包含降级标记
            if (result != null && result.containsKey("_fallback")) {
                log.info("成功触发降级逻辑: {}", result);
                return ResponseEntity.ok(result);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error test triggered exception: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error_handled");
            response.put("message", "处理过程中发生异常: " + e.getMessage());
            response.put("errorType", e.getClass().getName());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 测试错误处理 - 另一种方式
     */
    @GetMapping("/error-test-alt")
    public Object testErrorAlt() {
        log.info("Testing error scenario - alternative endpoint");
        
        try {
            // 这个调用应该会返回错误
            Map<String, Object> result = userServiceClient.testError();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error test alternative triggered exception: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error_alt_handled");
            response.put("message", "错误测试成功: 已处理异常");
            response.put("error", e.getMessage());
            response.put("errorType", e.getClass().getName());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 错误测试的本地降级方法
     */
    public ResponseEntity<?> errorTestFallback(Exception e) {
        log.info("错误测试本地降级方法被调用: {}", e.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("status", "local_fallback_handled");
        response.put("message", "错误测试成功: 触发了本地降级方法");
        response.put("error", e.getMessage());
        response.put("errorType", e.getClass().getName());
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok().body(response);
    }
    
    /**
     * 服务状态检查
     */
    @GetMapping("/status")
    public ResponseEntity<?> checkStatus() {
        log.info("Checking Feign client status");
        Map<String, Object> status = new HashMap<>();
        
        try {
            // 尝试调用本地服务
            Object localResult = localUserServiceClient.health();
            status.put("localService", "UP");
            status.put("localServiceDetails", localResult);
        } catch (Exception e) {
            log.error("Local service check failed: {}", e.getMessage());
            status.put("localService", "DOWN");
            status.put("localServiceError", e.getMessage());
        }
        
        try {
            // 尝试调用远程服务
            Object remoteResult = userServiceClient.health();
            status.put("remoteService", "UP");
            status.put("remoteServiceDetails", remoteResult);
        } catch (Exception e) {
            log.error("Remote service check failed: {}", e.getMessage());
            status.put("remoteService", "DOWN");
            status.put("remoteServiceError", e.getMessage());
        }
        
        status.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(status);
    }

    /**
     * 测试超时 - 短时间版本
     * 直接调用超时接口，但传入较短的超时时间参数
     */
    @GetMapping("/timeout-test-short")
    public Object testTimeoutShort() {
        log.info("Testing timeout with shorter duration");
        
        try {
            // 直接访问超时接口，但传入较短的超时参数（5秒）
            String url = "/api/users/timeout?duration=5000";
            log.info("Calling timeout endpoint with short duration: {}", url);
            
            // 使用本地客户端调用，避免复杂的超时处理
            Map<String, Object> result = localUserServiceClient.testTimeoutWithParam(5000L);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Short timeout test exception: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "short_timeout_handled");
            response.put("message", "短时间超时测试：" + e.getMessage());
            response.put("errorType", e.getClass().getName());
            return ResponseEntity.ok(response);
        }
    }
} 