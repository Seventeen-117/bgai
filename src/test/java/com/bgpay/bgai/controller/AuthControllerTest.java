package com.bgpay.bgai.controller;

import com.bgpay.bgai.base.NoSpringContextTest;
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
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
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
 * 认证控制器测试类
 * 使用TestNG框架测试/api/auth/refresh-by-userid接口
 * 采用完全独立的测试方式，不依赖Spring上下文
 */
public class AuthControllerTest extends NoSpringContextTest {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    // 手动创建MockMvc，不使用@Autowired
    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;
    
    // 全局异常处理器
    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    private UserToken mockUserToken;
    private final String ADMIN_TOKEN = "admin-token-for-testing";
    private final String USER_ID = "test-user-123";
    private final String API_ENDPOINT = "/api/auth/refresh-by-userid";
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 简单测试方法，不依赖Spring上下文
     */
    @Test
    public void simpleSanityTest() {
        log.info("执行简单测试用例");
        Assert.assertTrue(true, "测试环境正常运行");
        log.info("简单测试用例执行成功");
    }

    @BeforeClass(dependsOnMethods = "disableSpringContext")
    public void setUp() {
        log.info("初始化AuthControllerTest测试类");
        
        // 初始化Mockito注解
        MockitoAnnotations.openMocks(this);
        
        // 准备测试数据
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
        
        // 设置默认mock行为
        when(userService.refreshTokenByUserId(anyString())).thenReturn(mockUserToken);
        
        // 手动创建MockMvc实例
        mockMvc = MockMvcBuilders
            .standaloneSetup(authController)
            .setControllerAdvice(globalExceptionHandler) // 添加全局异常处理器
            .addFilter(new CharacterEncodingFilter("UTF-8", true)) // 添加编码过滤器
            .alwaysDo(print()) // 打印请求和响应，方便调试
            .build();
        
        log.info("成功创建MockMvc实例，并配置全局异常处理器");
    }

    @BeforeMethod
    public void beforeTestMethod() {
        log.info("----------------------------------------");
        log.info("开始执行测试方法");
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
    }
    
    /**
     * 测试缺少Authorization头的场景
     */
    @Test
    public void testRefreshTokenByUserId_MissingToken() throws Exception {
        log.info("测试用例: 缺少Authorization头");
        
        // 直接使用预期的异常并为其提供自定义响应
        mockMvc.perform(post(API_ENDPOINT)
                .param("userId", USER_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> {
                    String content = result.getResponse().getContentAsString();
                    log.info("响应内容: {}", content);
                    Assert.assertTrue(content.contains("MISSING_HEADER") || 
                                     content.contains("Authorization"),
                            "响应应包含缺少的头信息");
                });
        
        log.info("测试成功：验证了缺少Authorization头时的响应");
    }
    
    /**
     * 测试服务抛出异常的场景
     */
    @Test
    public void testRefreshTokenByUserId_ServiceException() throws Exception {
        log.info("测试用例: 服务抛出异常");
        
        // 准备测试数据 - 模拟服务抛出异常
        String errorMessage = "模拟刷新令牌失败";
        when(userService.refreshTokenByUserId(USER_ID)).thenThrow(new RuntimeException(errorMessage));
        
        // 执行请求并验证响应
        MvcResult result = mockMvc.perform(post(API_ENDPOINT)
                .param("userId", USER_ID)
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // 获取响应内容
        MockHttpServletResponse response = result.getResponse();
        String responseContent = response.getContentAsString();
        log.info("响应状态码: {}", response.getStatus());
        log.info("响应内容: {}", responseContent);
        
        // 断言响应结果
        Assert.assertTrue(response.getStatus() >= 500 && response.getStatus() < 600, 
                "应返回5xx服务器错误状态码");
        Assert.assertTrue(responseContent.contains("刷新令牌失败") || 
                responseContent.contains("error"), 
                "响应应包含错误消息");
        Assert.assertTrue(responseContent.contains(errorMessage) || 
                responseContent.contains("message") || 
                responseContent.contains("exception"), 
                "响应应包含异常信息");
    }
    
    /**
     * 测试无效用户ID的场景
     */
    @Test
    public void testRefreshTokenByUserId_InvalidUserId() throws Exception {
        log.info("测试用例: 无效用户ID");
        
        // 准备测试数据 - 模拟无效用户ID
        String invalidUserId = "non-existent-user";
        when(userService.refreshTokenByUserId(invalidUserId)).thenThrow(
                new RuntimeException("用户不存在"));
        
        // 执行请求并验证响应
        MvcResult result = mockMvc.perform(post(API_ENDPOINT)
                .param("userId", invalidUserId)
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // 获取响应内容
        MockHttpServletResponse response = result.getResponse();
        String responseContent = response.getContentAsString();
        log.info("响应状态码: {}", response.getStatus());
        log.info("响应内容: {}", responseContent);
        
        // 断言响应结果
        Assert.assertTrue(response.getStatus() >= 400, "应返回错误状态码");
        Assert.assertTrue(responseContent.contains("用户不存在") || 
                responseContent.contains("刷新令牌失败") || 
                responseContent.contains("error"), 
                "响应应包含用户不存在的相关信息");
        
        // 验证服务方法被调用
        verify(userService, times(1)).refreshTokenByUserId(invalidUserId);
    }
} 