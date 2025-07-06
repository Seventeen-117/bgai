package com.bgpay.bgai.controller;

import com.bgpay.bgai.base.AbstractWebMvcTest;
import com.bgpay.bgai.base.YamlDataProvider;
import com.bgpay.bgai.base.YamlSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * 基于YAML数据驱动的API测试示例
 */
@Feature("API测试")
public class YamlDataDrivenApiTest extends AbstractWebMvcTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 使用YAML数据驱动测试登录接口
     * 
     * @param testData YAML文件中的测试数据
     */
    @Test(dataProvider = "yamlData", dataProviderClass = YamlDataProvider.class)
    @Description("测试登录接口的各种场景")
    @Story("用户登录功能")
    public void testLogin(Map<String, Object> testData) throws Exception {
        // 从测试数据中提取信息
        String testId = (String) testData.get("id");
        String description = (String) testData.get("description");
        
        // 获取请求信息
        Map<String, Object> requestData = (Map<String, Object>) testData.get("request");
        String method = (String) requestData.get("method");
        Map<String, Object> headers = (Map<String, Object>) requestData.get("headers");
        Map<String, Object> body = (Map<String, Object>) requestData.get("body");
        
        // 获取预期响应信息
        Map<String, Object> expectedResponse = (Map<String, Object>) testData.get("expectedResponse");
        int statusCode = ((Integer) expectedResponse.get("statusCode")).intValue();
        List<String> bodyContains = (List<String>) expectedResponse.get("bodyContains");
        Map<String, Object> bodyEquals = (Map<String, Object>) expectedResponse.get("bodyEquals");
        
        // 构建请求
        String baseUrl = "/api/auth";
        String endpoint = "/login";
        MockHttpServletRequestBuilder requestBuilder;
        
        switch (method) {
            case "GET":
                requestBuilder = MockMvcRequestBuilders.get(baseUrl + endpoint);
                break;
            case "POST":
                requestBuilder = MockMvcRequestBuilders.post(baseUrl + endpoint);
                break;
            case "PUT":
                requestBuilder = MockMvcRequestBuilders.put(baseUrl + endpoint);
                break;
            case "DELETE":
                requestBuilder = MockMvcRequestBuilders.delete(baseUrl + endpoint);
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
        
        // 添加请求头
        if (headers != null) {
            for (Map.Entry<String, Object> header : headers.entrySet()) {
                requestBuilder.header(header.getKey(), header.getValue().toString());
            }
        }
        
        // 添加请求体
        if (body != null && (method.equals("POST") || method.equals("PUT"))) {
            requestBuilder.contentType(MediaType.APPLICATION_JSON);
            requestBuilder.content(objectMapper.writeValueAsString(body));
        }
        
        // 执行请求
        ResultActions resultActions = mockMvc.perform(requestBuilder);
        
        // 验证状态码
        resultActions.andExpect(MockMvcResultMatchers.status().is(statusCode));
        
        // 验证响应体包含的内容
        if (bodyContains != null) {
            for (String containText : bodyContains) {
                resultActions.andExpect(MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString(containText)));
            }
        }
        
        // 验证响应体的精确匹配
        if (bodyEquals != null) {
            for (Map.Entry<String, Object> entry : bodyEquals.entrySet()) {
                resultActions.andExpect(MockMvcResultMatchers.jsonPath("$." + entry.getKey()).value(entry.getValue()));
            }
        }
    }
    
    /**
     * 使用指定YAML文件测试用户API
     * 
     * @param testData YAML文件中的测试数据
     */
    @Test(dataProvider = "namedYamlData", dataProviderClass = YamlDataProvider.class)
    @YamlSource("api/users")
    @Description("测试用户API的各种场景")
    @Story("用户管理功能")
    public void testUserApi(Map<String, Object> testData) throws Exception {
        // 从测试数据中提取信息
        String testId = (String) testData.get("id");
        String description = (String) testData.get("description");
        String endpoint = (String) testData.get("endpoint");
        
        // 获取请求信息
        Map<String, Object> requestData = (Map<String, Object>) testData.get("request");
        String method = (String) requestData.get("method");
        Map<String, Object> headers = (Map<String, Object>) requestData.get("headers");
        Map<String, Object> body = (Map<String, Object>) requestData.get("body");
        Map<String, Object> pathVariables = (Map<String, Object>) requestData.get("pathVariables");
        
        // 获取预期响应信息
        Map<String, Object> expectedResponse = (Map<String, Object>) testData.get("expectedResponse");
        int statusCode = ((Integer) expectedResponse.get("statusCode")).intValue();
        List<String> bodyContains = (List<String>) expectedResponse.get("bodyContains");
        Map<String, Object> bodyEquals = (Map<String, Object>) expectedResponse.get("bodyEquals");
        
        // 构建请求
        String baseUrl = "/api/users";
        
        // 替换路径变量
        if (pathVariables != null) {
            for (Map.Entry<String, Object> entry : pathVariables.entrySet()) {
                endpoint = endpoint.replace("{" + entry.getKey() + "}", entry.getValue().toString());
            }
        }
        
        MockHttpServletRequestBuilder requestBuilder;
        
        switch (method) {
            case "GET":
                requestBuilder = MockMvcRequestBuilders.get(baseUrl + endpoint);
                break;
            case "POST":
                requestBuilder = MockMvcRequestBuilders.post(baseUrl + endpoint);
                break;
            case "PUT":
                requestBuilder = MockMvcRequestBuilders.put(baseUrl + endpoint);
                break;
            case "DELETE":
                requestBuilder = MockMvcRequestBuilders.delete(baseUrl + endpoint);
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
        
        // 添加请求头
        if (headers != null) {
            for (Map.Entry<String, Object> header : headers.entrySet()) {
                String value = header.getValue().toString();
                // 替换令牌变量
                if (value.contains("${token}")) {
                    value = value.replace("${token}", "sample-jwt-token-for-testing");
                }
                requestBuilder.header(header.getKey(), value);
            }
        }
        
        // 添加请求体
        if (body != null && (method.equals("POST") || method.equals("PUT"))) {
            requestBuilder.contentType(MediaType.APPLICATION_JSON);
            requestBuilder.content(objectMapper.writeValueAsString(body));
        }
        
        // 执行请求
        ResultActions resultActions = mockMvc.perform(requestBuilder);
        
        // 验证状态码
        resultActions.andExpect(MockMvcResultMatchers.status().is(statusCode));
        
        // 验证响应体包含的内容
        if (bodyContains != null) {
            for (String containText : bodyContains) {
                resultActions.andExpect(MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString(containText)));
            }
        }
        
        // 验证响应体的精确匹配
        if (bodyEquals != null) {
            for (Map.Entry<String, Object> entry : bodyEquals.entrySet()) {
                resultActions.andExpect(MockMvcResultMatchers.jsonPath("$." + entry.getKey()).value(entry.getValue()));
            }
        }
    }
} 