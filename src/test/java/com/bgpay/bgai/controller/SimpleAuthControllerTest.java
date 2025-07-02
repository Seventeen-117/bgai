package com.bgpay.bgai.controller;

import com.bgpay.bgai.entity.UserToken;
import com.bgpay.bgai.exception.GlobalExceptionHandler;
import com.bgpay.bgai.service.UserService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 简单的认证控制器测试类
 * 不依赖AbstractTestNGTest基类，单独验证刷新令牌接口
 */
public class SimpleAuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    @BeforeMethod
    public void setUp() {
        // 初始化Mockito注解
        MockitoAnnotations.openMocks(this);

        // 配置异常处理解析器
        final ExceptionHandlerExceptionResolver exceptionResolver = new ExceptionHandlerExceptionResolver();
        // 添加消息转换器
        exceptionResolver.setMessageConverters(Arrays.asList(new MappingJackson2HttpMessageConverter()));
        // 初始化异常处理器
        exceptionResolver.afterPropertiesSet();

        // 创建MockMvc
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setHandlerExceptionResolvers(Collections.singletonList(exceptionResolver))
                .alwaysDo(print())
                .build();

        // 准备测试数据
        UserToken mockUserToken = UserToken.builder()
                .userId("test-user-123")
                .username("testuser")
                .email("testuser@example.com")
                .accessToken("new-access-token-123")
                .tokenExpireTime(LocalDateTime.now().plusHours(24))
                .loginTime(LocalDateTime.now())
                .valid(true)
                .build();

        // 设置mock行为
        when(userService.refreshTokenByUserId(anyString())).thenReturn(mockUserToken);
    }

    /**
     * 测试成功刷新用户令牌
     */
    @Test
    public void testRefreshTokenByUserId_Success() throws Exception {
        mockMvc.perform(post("/api/auth/refresh-by-userid")
                .param("userId", "test-user-123")
                .header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("test-user-123"))
                .andExpect(jsonPath("$.accessToken").value("new-access-token-123"))
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    /**
     * 测试缺少userId参数的场景
     */
    @Test
    public void testRefreshTokenByUserId_MissingUserId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/refresh-by-userid")
                .header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andReturn();
        
        // 断言响应结果
        Assert.assertEquals(result.getResponse().getStatus(), HttpStatus.BAD_REQUEST.value(), 
                "缺少必需参数应返回400状态码");
        String responseContent = result.getResponse().getContentAsString();
        Assert.assertTrue(responseContent.contains("MISSING_PARAMETER") || 
                responseContent.contains("userId"), 
                "响应应包含参数错误信息");
    }

    /**
     * 测试异常情况
     */
    @Test
    public void testRefreshTokenByUserId_Exception() throws Exception {
        // 模拟异常
        String errorMessage = "测试异常";
        when(userService.refreshTokenByUserId("error-user")).thenThrow(new RuntimeException(errorMessage));

        MvcResult result = mockMvc.perform(post("/api/auth/refresh-by-userid")
                .param("userId", "error-user")
                .header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is5xxServerError())
                .andReturn();
                
        // 获取响应内容并验证
        String responseContent = result.getResponse().getContentAsString();
        
        // 输出实际的响应内容，帮助调试
        System.out.println("实际的响应内容: " + responseContent);
        
        // 更灵活的断言，接受多种可能的响应格式
        boolean hasErrorField = responseContent.contains("error") || 
                                responseContent.contains("errorCode");
        Assert.assertTrue(hasErrorField, "响应应包含错误标识字段");

    }
} 