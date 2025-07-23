package com.bgpay.bgai.config;

import com.bgpay.bgai.controller.AuthController;
import com.bgpay.bgai.controller.ApiKeyController;
import com.bgpay.bgai.controller.DynamicRouteController;
import com.bgpay.bgai.controller.SystemConfigController;
import com.bgpay.bgai.controller.MockUserServiceController;
import com.bgpay.bgai.entity.UserToken;
import com.bgpay.bgai.service.UserService;
import com.bgpay.bgai.service.ApiKeyService;
import com.bgpay.bgai.entity.ApiKey;
import com.bgpay.bgai.entity.ApiKeyInfo;
import com.bgpay.bgai.service.DynamicRouteService;
import org.springframework.cloud.gateway.route.RouteDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
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
        ReflectionTestUtils.setField(mockUserController, "testApiKey", "test-api-key-123");
        ReflectionTestUtils.setField(mockUserController, "apiKeyHeader", "X-API-Key");
        ReflectionTestUtils.setField(mockUserController, "timeoutDuration", 15000L);
        addTestUser(mockUserController, "1001", "testuser", "test@example.com");

        // 创建mock的UserService
        UserService mockUserService = Mockito.mock(UserService.class);
        UserToken mockUserToken = new UserToken();
        mockUserToken.setUserId("1001");
        mockUserToken.setUsername("testuser");
        mockUserToken.setAccessToken("valid-access-token");
        mockUserToken.setTokenExpireTime(LocalDateTime.now().plusHours(1));
        mockUserToken.setValid(true);
        Mockito.when(mockUserService.loginWithSSO(Mockito.anyString())).thenReturn(mockUserToken);
        Mockito.when(mockUserService.refreshToken("valid-refresh-token")).thenReturn(mockUserToken);
        Mockito.when(mockUserService.refreshToken("invalid-refresh-token")).thenThrow(new RuntimeException("Invalid refresh token"));
        Mockito.when(mockUserService.validateToken("valid-token")).thenReturn(mockUserToken);
        Mockito.when(mockUserService.validateToken("invalid-token")).thenReturn(null);
        Mockito.doNothing().when(mockUserService).logout(Mockito.anyString());
        Mockito.when(mockUserService.validateToken(Mockito.anyString())).thenReturn(mockUserToken);

        // mock ApiKeyService
        ApiKeyService mockApiKeyService = Mockito.mock(ApiKeyService.class);
        ApiKeyInfo mockApiKeyInfo = ApiKeyInfo.builder()
                .apiKey("mock-key-value")
                .clientId("default-client")
                .clientName("test-client")
                .description("test-desc")
                .active(true)
                .build();
        Mockito.when(mockApiKeyService.generateApiKey(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(mockApiKeyInfo);
        Mockito.doNothing().when(mockApiKeyService).revokeApiKey(Mockito.anyString());
        Mockito.when(mockApiKeyService.getAllApiKeys()).thenReturn(java.util.Collections.emptyList());
        Mockito.when(mockApiKeyService.getApiKeyInfo(Mockito.anyString())).thenReturn(new ApiKey());
        Mockito.doNothing().when(mockApiKeyService).updateApiKeyStatus(Mockito.anyString(), Mockito.anyBoolean());
        Mockito.when(mockApiKeyService.validateApiKeyStatus(Mockito.anyString())).thenReturn(
                new ApiKeyService.ApiKeyValidationResult(
                        ApiKeyService.ApiKeyStatus.VALID,
                        LocalDateTime.now().plusDays(1),
                        null,
                        "default-client"
                )
        );

        // mock DynamicRouteService
        DynamicRouteService mockDynamicRouteService = Mockito.mock(DynamicRouteService.class);
        RouteDefinition mockRoute = new RouteDefinition();
        mockRoute.setId("route-001");
        mockRoute.setUri(java.net.URI.create("http://example.com"));
        Mockito.when(mockDynamicRouteService.getRoute("route-001")).thenReturn(Mono.just(mockRoute));
        Mockito.when(mockDynamicRouteService.getRoute("non-existent-route")).thenReturn(Mono.empty());
        Mockito.when(mockDynamicRouteService.getRoutes()).thenReturn(Flux.just(mockRoute));
        Mockito.when(mockDynamicRouteService.add(Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(mockDynamicRouteService.update(Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(mockDynamicRouteService.delete(Mockito.anyString())).thenReturn(Mono.empty());
        Mockito.when(mockDynamicRouteService.refreshRoutes()).thenReturn(Mono.empty());

        // 创建真实DynamicRouteController实例
        com.bgpay.bgai.controller.DynamicRouteController dynamicRouteController = new com.bgpay.bgai.controller.DynamicRouteController();
        org.springframework.test.util.ReflectionTestUtils.setField(dynamicRouteController, "dynamicRouteService", mockDynamicRouteService);

        // 创建真实ApiKeyController实例
        com.bgpay.bgai.controller.ApiKeyController apiKeyController = new com.bgpay.bgai.controller.ApiKeyController(mockApiKeyService, mockUserService);

        // 创建真实的AuthController实例并设置依赖
        AuthController authController = new AuthController();
        ReflectionTestUtils.setField(authController, "userService", mockUserService);
        ReflectionTestUtils.setField(authController, "environment", Mockito.mock(org.springframework.core.env.Environment.class));
        ReflectionTestUtils.setField(authController, "clientId", "test-client-id");
        ReflectionTestUtils.setField(authController, "authorizeUrl", "https://test-sso.example.com/oauth2/authorize");
        ReflectionTestUtils.setField(authController, "redirectUri", "http://localhost:8688/api/auth/callback");
        ReflectionTestUtils.setField(authController, "serverPort", 8688);
        ReflectionTestUtils.setField(authController, "serverInitialized", true);

        // mock ReactiveChatController 依赖
        com.bgpay.bgai.service.deepseek.ReactiveFileProcessor mockFileProcessor = Mockito.mock(com.bgpay.bgai.service.deepseek.ReactiveFileProcessor.class);
        com.bgpay.bgai.service.ApiConfigService mockApiConfigService = Mockito.mock(com.bgpay.bgai.service.ApiConfigService.class);
        com.bgpay.bgai.service.deepseek.DeepSeekService mockDeepSeekService = Mockito.mock(com.bgpay.bgai.service.deepseek.DeepSeekService.class);
        org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory mockCircuitBreakerFactory = Mockito.mock(org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory.class);
        com.bgpay.bgai.service.impl.FallbackService mockFallbackService = Mockito.mock(com.bgpay.bgai.service.impl.FallbackService.class);
        com.bgpay.bgai.transaction.TransactionCoordinator mockTransactionCoordinator = Mockito.mock(com.bgpay.bgai.transaction.TransactionCoordinator.class);
        com.bgpay.bgai.web.RequestAttributesProvider mockAttributesProvider = Mockito.mock(com.bgpay.bgai.web.RequestAttributesProvider.class);
        // mockUserService, mockApiKeyService 已有

        // 创建真实 ReactiveChatController 实例
        com.bgpay.bgai.controller.ReactiveChatController reactiveChatController = new com.bgpay.bgai.controller.ReactiveChatController(
            mockFileProcessor,
            mockApiConfigService,
            mockDeepSeekService,
            mockCircuitBreakerFactory,
            mockFallbackService,
            mockTransactionCoordinator,
            mockAttributesProvider,
            mockUserService,
            mockApiKeyService
        );

        return MockMvcBuilders.standaloneSetup(
            authController,
            apiKeyController,
            dynamicRouteController,
            Mockito.mock(SystemConfigController.class),
            mockUserController,
            reactiveChatController // <--- 新增
        )
        .defaultRequest(MockMvcRequestBuilders.get("/").characterEncoding("UTF-8"))
        .build();
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