package com.bgpay.bgai.controller;

import com.bgpay.bgai.entity.UserToken;
import com.bgpay.bgai.exception.GlobalExceptionHandler;
import com.bgpay.bgai.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证控制器独立测试
 * 不使用Spring上下文，完全使用MockMvc进行测试
 */
public class StandaloneAuthControllerTest {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;
    
    private GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    private UserToken mockUserToken;
    private final String ADMIN_TOKEN = "admin-token-for-testing";
    private final String USER_ID = "test-user-123";
    private final String API_ENDPOINT = "/api/auth/refresh-by-userid";
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeMethod
    public void setUp() {
        log.info("初始化测试环境");
        // 初始化Mockito注解
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
        
        // 构建MockMvc
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(globalExceptionHandler)
                .addFilter(new CharacterEncodingFilter("UTF-8", true))
                .alwaysDo(print())
                .build();
        
        log.info("测试环境初始化完成");
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
                .andReturn();
        
        // 获取响应内容
        MockHttpServletResponse response = result.getResponse();
        String responseContent = response.getContentAsString();
        log.info("响应状态码: {}", response.getStatus());
        log.info("响应内容: {}", responseContent);
        
        // 断言响应结果
        Assert.assertEquals(response.getStatus(), HttpStatus.BAD_REQUEST.value(),
                "缺少必需参数应返回400状态码");
        Assert.assertTrue(responseContent.contains("MISSING_PARAMETER") || 
                responseContent.contains("userId"),
                "响应内容应包含参数名称");
        
        // 确认mock没有被调用，因为参数缺失
        verify(userService, never()).refreshTokenByUserId(anyString());
    }
    
    /**
     * 测试成功刷新用户令牌的场景
     */
    @Test
    public void testRefreshTokenByUserId_Success() throws Exception {
        log.info("测试用例: 成功刷新用户令牌");
        
        // 执行请求并验证响应
        MvcResult result = mockMvc.perform(post(API_ENDPOINT)
                .param("userId", USER_ID)
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.accessToken").value("new-access-token-123"))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andReturn();

        // 获取响应内容
        MockHttpServletResponse response = result.getResponse();
        String responseContent = response.getContentAsString();
        log.info("响应内容: {}", responseContent);
        
        // 验证服务方法被调用
        verify(userService, times(1)).refreshTokenByUserId(USER_ID);
        
        // 断言响应结果
        Assert.assertEquals(response.getStatus(), HttpStatus.OK.value());
        Assert.assertTrue(responseContent.contains(USER_ID));
        Assert.assertTrue(responseContent.contains("new-access-token-123"));
        Assert.assertTrue(responseContent.contains(mockUserToken.getTokenExpireTime().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
    }
} 