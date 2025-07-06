package com.bgpay.bgai.controller;

import com.bgpay.bgai.base.AbstractWebMvcTest;
import io.qameta.allure.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 示例控制器测试类
 * 展示如何使用测试框架进行控制器测试
 */
@Epic("API测试")
@Feature("示例控制器")
@Owner("测试团队")
public class SampleControllerTest extends AbstractWebMvcTest {

    /**
     * 测试获取用户列表接口
     */
    @Test(groups = {"controller-tests", "user-api"})
    @Story("用户管理")
    @Description("测试获取用户列表接口，验证返回状态码和响应格式")
    @Severity(SeverityLevel.CRITICAL)
    @Issue("BGAI-123")
    @TmsLink("TC-456")
    public void testGetUsers() throws Exception {
        logStep("发送GET请求到/api/users");
        
        performGet("/api/users")
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
            .andExpect(jsonPath("$.success", is(true)));
        
        logStep("验证响应成功");
    }
    
    /**
     * 测试获取单个用户接口
     */
    @Test(groups = {"controller-tests", "user-api"})
    @Story("用户管理")
    @Description("测试获取单个用户接口，验证返回的用户信息正确")
    @Severity(SeverityLevel.NORMAL)
    public void testGetUserById() throws Exception {
        String userId = "123";
        logStep("发送GET请求到/api/users/{id}");
        
        performGet("/api/users/{id}", userId)
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.id", is(userId)))
            .andExpect(jsonPath("$.data.name", notNullValue()))
            .andExpect(jsonPath("$.success", is(true)));
        
        logStep("验证用户信息正确");
    }
    
    /**
     * 测试创建用户接口
     */
    @Test(groups = {"controller-tests", "user-api"})
    @Story("用户管理")
    @Description("测试创建用户接口，验证用户创建成功")
    @Severity(SeverityLevel.CRITICAL)
    public void testCreateUser() throws Exception {
        // 创建测试数据
        String requestBody = "{"
            + "\"name\": \"测试用户\","
            + "\"email\": \"test@example.com\","
            + "\"age\": 30"
            + "}";
        
        logStep("发送POST请求到/api/users");
        
        performPost("/api/users", requestBody)
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.id", notNullValue()))
            .andExpect(jsonPath("$.data.name", is("测试用户")))
            .andExpect(jsonPath("$.data.email", is("test@example.com")))
            .andExpect(jsonPath("$.success", is(true)));
        
        logStep("验证用户创建成功");
    }
    
    /**
     * 测试更新用户接口
     */
    @Test(groups = {"controller-tests", "user-api"})
    @Story("用户管理")
    @Description("测试更新用户接口，验证用户信息更新成功")
    @Severity(SeverityLevel.NORMAL)
    public void testUpdateUser() throws Exception {
        String userId = "123";
        // 创建测试数据
        String requestBody = "{"
            + "\"name\": \"更新的用户名\","
            + "\"email\": \"updated@example.com\","
            + "\"age\": 35"
            + "}";
        
        logStep("发送PUT请求到/api/users/{id}");
        
        performPut("/api/users/" + userId, requestBody)
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.id", is(userId)))
            .andExpect(jsonPath("$.data.name", is("更新的用户名")))
            .andExpect(jsonPath("$.data.email", is("updated@example.com")))
            .andExpect(jsonPath("$.success", is(true)));
        
        logStep("验证用户信息更新成功");
    }
    
    /**
     * 测试删除用户接口
     */
    @Test(groups = {"controller-tests", "user-api"})
    @Story("用户管理")
    @Description("测试删除用户接口，验证用户删除成功")
    @Severity(SeverityLevel.NORMAL)
    public void testDeleteUser() throws Exception {
        String userId = "123";
        
        logStep("发送DELETE请求到/api/users/{id}");
        
        performDelete("/api/users/" + userId)
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success", is(true)));
        
        logStep("验证用户删除成功");
        
        // 验证用户已被删除
        performGet("/api/users/{id}", userId)
            .andExpect(status().isNotFound());
        
        logStep("验证用户不存在");
    }
} 