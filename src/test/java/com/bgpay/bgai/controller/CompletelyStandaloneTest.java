package com.bgpay.bgai.controller;

import com.bgpay.bgai.base.NoSpringContextTest;
import com.bgpay.bgai.entity.UserToken;
import com.bgpay.bgai.exception.GlobalExceptionHandler;
import com.bgpay.bgai.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * 完全独立的测试类
 * 不依赖Spring上下文，使用MockMvc独立模式
 */
public class CompletelyStandaloneTest extends NoSpringContextTest {
    
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;
    
    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    private UserToken mockUserToken;
    private final String ADMIN_TOKEN = "admin-token-for-testing";
    private final String USER_ID = "test-user-123";
    private final String API_ENDPOINT = "/api/auth/refresh-by-userid";
    
    /**
     * 简单测试方法，不依赖任何框架
     */
    @Test
    public void simpleSanityTest() {
        log.info("执行简单测试用例");
        Assert.assertTrue(true, "测试环境正常运行");
        log.info("简单测试用例执行成功");
    }
    
    @BeforeClass(dependsOnMethods = "disableSpringContext")
    public void setUp() {
        log.info("初始化测试环境");
        MockitoAnnotations.openMocks(this);
        
        // 创建模拟用户令牌
        LocalDateTime expireTime = LocalDateTime.now().plusHours(24);
        mockUserToken = UserToken.builder()
                .userId(USER_ID)
                .username("testuser")
                .email("testuser@example.com")
                .accessToken("new-access-token-123")
                .tokenExpireTime(expireTime)
                .loginTime(LocalDateTime.now())
                .valid(true)
                .build();
                
        // 设置Mock行为
        when(userService.refreshTokenByUserId(anyString())).thenReturn(mockUserToken);
    }
    
    @BeforeMethod
    public void setupMockMvc() {
        // 构建MockMvc - 完全独立模式
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(globalExceptionHandler)
                .build();
    }
    
    /**
     * 测试缺少userId参数的场景
     */
    @Test
    public void testRefreshTokenByUserId_MissingUserId() throws Exception {
        log.info("测试用例: 缺少userId参数");
        
        // 执行请求并验证响应
        MvcResult result = mockMvc.perform(post(API_ENDPOINT)
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andReturn();
        
        // 获取响应内容
        MockHttpServletResponse response = result.getResponse();
        String responseContent = response.getContentAsString();
        log.info("响应状态码: {}", response.getStatus());
        log.info("响应内容: {}", responseContent);
        
        // 断言响应结果
        Assert.assertEquals(response.getStatus(), HttpStatus.BAD_REQUEST.value(),
                "缺少必需参数应返回400状态码");
    }
} 