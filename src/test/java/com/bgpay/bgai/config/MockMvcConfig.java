package com.bgpay.bgai.config;

import com.bgpay.bgai.controller.AuthController;
import com.bgpay.bgai.controller.ApiKeyController;
import com.bgpay.bgai.controller.DynamicRouteController;
import com.bgpay.bgai.controller.SystemConfigController;
import com.bgpay.bgai.controller.MockUserServiceController;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 提供MockMvc bean的配置类
 * 解决测试类中MockMvc依赖注入失败问题
 */
@TestConfiguration
public class MockMvcConfig {

    /**
     * 提供MockMvc bean
     * 使用standaloneSetup避免ServletContext依赖
     */
    @Bean
    @Primary
    public MockMvc mockMvc() {
        // 创建真实的MockUserServiceController实例
        MockUserServiceController mockUserController = new MockUserServiceController();
        
        // 手动设置必要的属性值，避免@Value注入问题
        ReflectionTestUtils.setField(mockUserController, "testApiKey", "test-api-key-123");
        ReflectionTestUtils.setField(mockUserController, "apiKeyHeader", "X-API-Key");
        ReflectionTestUtils.setField(mockUserController, "timeoutDuration", 15000L);
        
        // 添加测试所需的用户数据
        addTestUser(mockUserController, "1001", "testuser", "test@example.com");
        
        return MockMvcBuilders.standaloneSetup(
            Mockito.mock(AuthController.class),
            Mockito.mock(ApiKeyController.class),
            Mockito.mock(DynamicRouteController.class),
            Mockito.mock(SystemConfigController.class),
            mockUserController
        ).build();
    }
    
    /**
     * 添加测试用户数据
     */
    private void addTestUser(MockUserServiceController controller, String id, String username, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("id", id);
        user.put("username", username);
        user.put("name", username);
        user.put("email", email);
        user.put("active", true);
        user.put("roles", java.util.Arrays.asList("USER"));
        user.put("createdAt", new Date());
        user.put("_mock", true);
        
        // 使用反射设置用户数据
        Map<String, Map<String, Object>> users = (Map<String, Map<String, Object>>) 
            ReflectionTestUtils.getField(controller, "users");
        users.put(id, user);
    }
} 