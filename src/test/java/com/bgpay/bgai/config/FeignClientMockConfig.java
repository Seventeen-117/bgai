package com.bgpay.bgai.config;

import com.bgpay.bgai.feign.LocalUserServiceClient;
import com.bgpay.bgai.feign.UserServiceClient;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.*;

/**
 * 提供Feign客户端的Mock实现
 * 解决测试时FeignDemoController需要UserServiceClient和LocalUserServiceClient的问题
 */
@Configuration
public class FeignClientMockConfig {

    /**
     * 提供UserServiceClient的Mock实现
     */
    @Bean
    @Primary
    public UserServiceClient userServiceClient() {
        UserServiceClient mockClient = Mockito.mock(UserServiceClient.class);
        
        // 配置mock行为
        Mockito.when(mockClient.listUsers()).thenReturn(createTestUsers());
        Mockito.when(mockClient.getUserById(Mockito.anyString())).thenAnswer(invocation -> {
            String userId = invocation.getArgument(0);
            return createTestUser(userId);
        });
        Mockito.when(mockClient.createUser(Mockito.anyMap())).thenAnswer(invocation -> {
            Map<String, Object> userData = invocation.getArgument(0);
            Map<String, Object> result = new HashMap<>(userData);
            result.put("id", UUID.randomUUID().toString());
            result.put("createdAt", new Date().toString());
            return result;
        });
        Mockito.when(mockClient.testError()).thenReturn(createErrorResponse());
        Mockito.when(mockClient.testTimeout()).thenReturn(createTimeoutResponse());
        
        return mockClient;
    }
    
    /**
     * 提供LocalUserServiceClient的Mock实现
     */
    @Bean
    @Primary
    public LocalUserServiceClient localUserServiceClient() {
        LocalUserServiceClient mockClient = Mockito.mock(LocalUserServiceClient.class);
        
        // 配置与UserServiceClient相同的mock行为
        Mockito.when(mockClient.listUsers()).thenReturn(createTestUsers());
        Mockito.when(mockClient.getUserById(Mockito.anyString())).thenAnswer(invocation -> {
            String userId = invocation.getArgument(0);
            return createTestUser(userId);
        });
        Mockito.when(mockClient.createUser(Mockito.anyMap())).thenAnswer(invocation -> {
            Map<String, Object> userData = invocation.getArgument(0);
            Map<String, Object> result = new HashMap<>(userData);
            result.put("id", UUID.randomUUID().toString());
            result.put("createdAt", new Date().toString());
            return result;
        });
        Mockito.when(mockClient.testError()).thenReturn(createErrorResponse());
        Mockito.when(mockClient.testTimeout()).thenReturn(createTimeoutResponse());
        
        return mockClient;
    }
    
    /**
     * 创建测试用户列表
     */
    private List<Map<String, Object>> createTestUsers() {
        List<Map<String, Object>> users = new ArrayList<>();
        users.add(createTestUser("1"));
        users.add(createTestUser("2"));
        users.add(createTestUser("3"));
        return users;
    }
    
    /**
     * 创建单个测试用户
     */
    private Map<String, Object> createTestUser(String id) {
        Map<String, Object> user = new HashMap<>();
        user.put("id", id);
        user.put("username", "testuser" + id);
        user.put("name", "Test User " + id);
        user.put("email", "testuser" + id + "@example.com");
        return user;
    }
    
    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error_test");
        response.put("message", "This is a test error response");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    
    /**
     * 创建超时响应
     */
    private Map<String, Object> createTimeoutResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "timeout_test");
        response.put("message", "This is a test timeout response");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
} 