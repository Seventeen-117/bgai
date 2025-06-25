package com.bgpay.bgai.controller;

import com.bgpay.bgai.entity.User;
import com.bgpay.bgai.feign.LocalUserServiceClient;
import com.bgpay.bgai.feign.UserServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Feign测试专用控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/feign-test")
public class FeignTestController {

    @Autowired
    private UserServiceClient userServiceClient;
    
    @Autowired
    private LocalUserServiceClient localUserServiceClient;

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("message", "Feign测试控制器运行正常");
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }

    /**
     * 测试用户服务降级
     * 此接口会尝试调用可能不存在的用户服务，触发降级逻辑
     */
    @GetMapping("/test-fallback/{id}")
    public ResponseEntity<Map<String, Object>> testFallback(@PathVariable String id) {
        log.info("测试Feign客户端降级功能: 用户ID = {}", id);
        
        try {
            // 尝试使用远程服务客户端，可能会失败
            Map<String, Object> user = userServiceClient.getUserById(id);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("user", user);
            result.put("fallbackTriggered", false);
            result.put("clientType", "remote");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("远程服务调用失败，尝试本地服务: {}", e.getMessage());
            
            try {
                // 尝试使用本地服务客户端
                Map<String, Object> user = localUserServiceClient.getUserById(id);
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("user", user);
                result.put("fallbackTriggered", true);
                result.put("clientType", "local");
                
                return ResponseEntity.ok(result);
            } catch (Exception localException) {
                log.error("本地服务也调用失败: {}", localException.getMessage());
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", localException.getMessage());
                result.put("fallbackTriggered", true);
                result.put("clientType", "none");
                
                return ResponseEntity.ok(result);
            }
        }
    }
    
    /**
     * 测试创建用户 - 使用本地客户端
     */
    @PostMapping("/test-create")
    public ResponseEntity<Map<String, Object>> testCreateUser(@RequestBody Map<String, Object> userData) {
        log.info("测试Feign客户端创建用户: {}", userData);
        
        try {
            // 直接使用本地服务客户端
            Map<String, Object> createdUser = localUserServiceClient.createUser(userData);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("createdUser", createdUser);
            result.put("clientType", "local");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("创建用户失败: {}", e.getMessage());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * 测试本地服务状态
     */
    @GetMapping("/local-status")
    public ResponseEntity<Map<String, Object>> localStatus() {
        log.info("检查本地Feign客户端状态");
        
        try {
            // 使用本地服务客户端
            var users = localUserServiceClient.listUsers();
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "UP");
            result.put("userServiceConnected", true);
            result.put("usersCount", users.size());
            result.put("clientType", "local");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("本地Feign客户端连接失败: {}", e.getMessage());
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "DOWN");
            result.put("userServiceConnected", false);
            result.put("error", e.getMessage());
            
            return ResponseEntity.ok(result);
        }
    }
} 