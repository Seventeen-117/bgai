package com.bgpay.bgai.controller;

import com.bgpay.bgai.entity.User;
import com.bgpay.bgai.feign.LocalUserServiceClient;
import com.bgpay.bgai.feign.UserServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Feign示例控制器
 * 用于演示OpenFeign的使用
 */
@Slf4j
@RestController
@RequestMapping("/api/feign-demo")
public class FeignDemoController {

    private final UserServiceClient userServiceClient;
    private final LocalUserServiceClient localUserServiceClient;
    
    @Value("${bgai.api-key.test-key:test-api-key-123}")
    private String testApiKey;

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
     * 测试超时
     */
    @GetMapping("/timeout-test")
    public ResponseEntity<?> testTimeout() {
        log.info("Testing timeout scenario");
        try {
            // 这个调用应该会超时
            Map<String, Object> result = userServiceClient.testTimeout();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Timeout test triggered exception: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("status", "timeout_handled");
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 测试错误处理
     */
    @GetMapping("/error-test")
    public ResponseEntity<?> testError() {
        log.info("Testing error scenario");
        try {
            // 这个调用应该会返回错误
            Map<String, Object> result = userServiceClient.testError();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error test triggered exception: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("status", "error_handled");
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
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
} 