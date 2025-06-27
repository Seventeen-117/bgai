package com.bgpay.bgai.controller;

import com.bgpay.bgai.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 模拟用户服务控制器
 * 提供用于测试的模拟用户API
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
public class MockUserServiceController {
    
    @Value("${bgai.api-key.test-key:test-api-key-123}")
    private String testApiKey;
    
    @Value("${bgai.api-key.header-name:X-API-Key}")
    private String apiKeyHeader;
    
    @Value("${bgai.mock-service.timeout-duration:15000}")
    private long timeoutDuration;

    // 模拟用户数据存储
    private final Map<String, Map<String, Object>> users = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    /**
     * 构造函数，初始化一些模拟用户数据
     */
    public MockUserServiceController() {
        // 添加一些默认用户
        addMockUser("1", "john.doe", "John Doe", "john.doe@example.com", true);
        addMockUser("2", "jane.smith", "Jane Smith", "jane.smith@example.com", true);
        addMockUser("3", "bob.johnson", "Bob Johnson", "bob.johnson@example.com", false);
    }

    /**
     * 添加模拟用户数据
     */
    private void addMockUser(String id, String username, String name, String email, boolean active) {
        Map<String, Object> user = new HashMap<>();
        user.put("id", id);
        user.put("username", username);
        user.put("name", name);
        user.put("email", email);
        user.put("active", active);
        user.put("roles", Arrays.asList("USER"));
        user.put("createdAt", new Date());
        users.put(id, user);
    }

    /**
     * 身份验证检查
     */
    private ResponseEntity<?> checkAuthentication(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "API Key is required"));
        }
        
        if (!testApiKey.equals(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "Invalid API Key"));
        }
        
        return null; // 验证通过
    }

    /**
     * 获取所有用户
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers(@RequestHeader(name = "X-API-Key", required = false) String apiKey) {
        log.info("Mock UserService - Get all users request received");
        
        ResponseEntity<?> authCheck = checkAuthentication(apiKey);
        if (authCheck != null) {
            return authCheck;
        }
        
        return ResponseEntity.ok(new HashMap<String, Object>() {{
            put("users", new ArrayList<>(users.values()));
            put("total", users.size());
            put("_mock", true);
        }});
    }

    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(
            @PathVariable String id,
            @RequestHeader(name = "X-API-Key", required = false) String apiKey) {
        log.info("Mock UserService - Get user by ID request received: {}", id);
        
        ResponseEntity<?> authCheck = checkAuthentication(apiKey);
        if (authCheck != null) {
            return authCheck;
        }
        
        if (users.containsKey(id)) {
            Map<String, Object> user = new HashMap<>(users.get(id));
            user.put("_mock", true);
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", "User not found with ID: " + id));
        }
    }

    /**
     * 创建新用户
     */
    @PostMapping
    public ResponseEntity<?> createUser(
            @RequestBody Map<String, Object> userData,
            @RequestHeader(name = "X-API-Key", required = false) String apiKey) {
        log.info("Mock UserService - Create user request received: {}", userData);
        
        ResponseEntity<?> authCheck = checkAuthentication(apiKey);
        if (authCheck != null) {
            return authCheck;
        }
        
        String id = String.valueOf(users.size() + 1);
        userData.put("id", id);
        userData.put("createdAt", new Date());
        userData.put("_mock", true);
        
        users.put(id, userData);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(userData);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable String id, 
            @RequestBody Map<String, Object> userData,
            @RequestHeader(name = "X-API-Key", required = false) String apiKey) {
        log.info("Mock UserService - Update user request received: {} with data: {}", id, userData);
        
        ResponseEntity<?> authCheck = checkAuthentication(apiKey);
        if (authCheck != null) {
            return authCheck;
        }
        
        if (users.containsKey(id)) {
            Map<String, Object> existingUser = users.get(id);
            existingUser.putAll(userData);
            existingUser.put("updatedAt", new Date());
            existingUser.put("_mock", true);
            
            return ResponseEntity.ok(existingUser);
        } else {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", "User not found with ID: " + id));
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(
            @PathVariable String id,
            @RequestHeader(name = "X-API-Key", required = false) String apiKey) {
        log.info("Mock UserService - Delete user request received: {}", id);
        
        ResponseEntity<?> authCheck = checkAuthentication(apiKey);
        if (authCheck != null) {
            return authCheck;
        }
        
        if (users.containsKey(id)) {
            users.remove(id);
            return ResponseEntity
                    .ok(Collections.singletonMap("message", "User deleted successfully"));
        } else {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", "User not found with ID: " + id));
        }
    }

    /**
     * 模拟服务健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "mock-user-service");
        health.put("timestamp", new Date());
        return ResponseEntity.ok(health);
    }
    
    /**
     * 模拟服务错误
     */
    @GetMapping("/error")
    public ResponseEntity<Map<String, Object>> error() {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Simulated server error");
        errorResponse.put("code", "INTERNAL_SERVER_ERROR");
        errorResponse.put("message", "这是一个模拟的服务器错误，用于测试降级逻辑");
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
    
    /**
     * 模拟服务超时
     * 可通过请求参数或配置属性控制超时时间
     */
    @GetMapping("/timeout")
    public ResponseEntity<Map<String, Object>> timeout(
            @RequestParam(required = false) Long duration) throws InterruptedException {
        // 使用请求参数或配置的超时时间
        long sleepTime = duration != null ? duration : timeoutDuration;
        log.info("Simulating timeout for {} ms...", sleepTime);
        
        // 添加一些随机性，避免总是相同的超时时间
        long actualSleep = sleepTime + (long)(Math.random() * 500);
        Thread.sleep(actualSleep);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This message should never be returned normally");
        response.put("sleptFor", actualSleep);
        response.put("timestamp", new Date());
        return ResponseEntity.ok(response);
    }
} 