package com.bgpay.bgai.controller;

import com.bgpay.bgai.base.AbstractWebFluxTest;
import com.bgpay.bgai.base.YamlDataProvider;
import com.bgpay.bgai.base.YamlSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * 基于YAML数据驱动的WebFlux API测试示例
 */
@Feature("WebFlux API测试")
public class ReactiveYamlDataDrivenApiTest extends AbstractWebFluxTest {

    @Autowired
    private WebTestClient webTestClient;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 使用YAML数据驱动测试聊天接口
     * 
     * @param testData YAML文件中的测试数据
     */
    @Test(dataProvider = "yamlData", dataProviderClass = YamlDataProvider.class)
    @Description("测试聊天接口的各种场景")
    @Story("聊天功能")
    public void testChatApi(Map<String, Object> testData) {
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
        String baseUrl = "/api/chat";
        String endpoint = "/completions";
        WebTestClient.RequestHeadersSpec<?> requestSpec;
        
        switch (method) {
            case "GET":
                requestSpec = webTestClient.get().uri(baseUrl + endpoint);
                break;
            case "POST":
                requestSpec = webTestClient.post().uri(baseUrl + endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(body));
                break;
            case "PUT":
                requestSpec = webTestClient.put().uri(baseUrl + endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(body));
                break;
            case "DELETE":
                requestSpec = webTestClient.delete().uri(baseUrl + endpoint);
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
        
        // 添加请求头
        if (headers != null) {
            for (Map.Entry<String, Object> header : headers.entrySet()) {
                requestSpec = requestSpec.header(header.getKey(), header.getValue().toString());
            }
        }
        
        // 执行请求并验证
        WebTestClient.ResponseSpec responseSpec = requestSpec.exchange();
        
        // 验证状态码
        responseSpec.expectStatus().isEqualTo(statusCode);
        
        // 验证响应体
        if (bodyContains != null || bodyEquals != null) {
            responseSpec.expectBody()
                .consumeWith(response -> {
                    String responseBody = new String(response.getResponseBody());
                    
                    // 验证响应体包含的内容
                    if (bodyContains != null) {
                        for (String containText : bodyContains) {
                            assert responseBody.contains(containText) : 
                                "Expected response to contain '" + containText + "' but was: " + responseBody;
                        }
                    }
                    
                    // 验证响应体的精确匹配（这里简化处理，实际可能需要更复杂的JSON解析）
                    if (bodyEquals != null) {
                        for (Map.Entry<String, Object> entry : bodyEquals.entrySet()) {
                            // 简化处理，实际实现可能需要使用JsonPath或其他方式验证
                            String expectedValue = entry.getValue().toString();
                            assert responseBody.contains("\"" + entry.getKey() + "\":" + expectedValue) || 
                                   responseBody.contains("\"" + entry.getKey() + "\": " + expectedValue) : 
                                "Expected response to contain '" + entry.getKey() + ":" + expectedValue + "' but was: " + responseBody;
                        }
                    }
                });
        }
    }
    
    /**
     * 使用指定YAML文件测试反应式用户API
     * 
     * @param testData YAML文件中的测试数据
     */
    @Test(dataProvider = "namedYamlData", dataProviderClass = YamlDataProvider.class)
    @YamlSource("api/reactive-users")
    @Description("测试反应式用户API的各种场景")
    @Story("反应式用户管理功能")
    public void testReactiveUserApi(Map<String, Object> testData) {
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
        String baseUrl = "/api/reactive/users";
        
        // 替换路径变量
        if (pathVariables != null) {
            for (Map.Entry<String, Object> entry : pathVariables.entrySet()) {
                endpoint = endpoint.replace("{" + entry.getKey() + "}", entry.getValue().toString());
            }
        }
        
        WebTestClient.RequestHeadersSpec<?> requestSpec;
        
        switch (method) {
            case "GET":
                requestSpec = webTestClient.get().uri(baseUrl + endpoint);
                break;
            case "POST":
                requestSpec = webTestClient.post().uri(baseUrl + endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(body));
                break;
            case "PUT":
                requestSpec = webTestClient.put().uri(baseUrl + endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(body));
                break;
            case "DELETE":
                requestSpec = webTestClient.delete().uri(baseUrl + endpoint);
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
                requestSpec = requestSpec.header(header.getKey(), value);
            }
        }
        
        // 执行请求并验证
        WebTestClient.ResponseSpec responseSpec = requestSpec.exchange();
        
        // 验证状态码
        responseSpec.expectStatus().isEqualTo(statusCode);
        
        // 验证响应体
        if (bodyContains != null || bodyEquals != null) {
            responseSpec.expectBody()
                .consumeWith(response -> {
                    String responseBody = new String(response.getResponseBody());
                    
                    // 验证响应体包含的内容
                    if (bodyContains != null) {
                        for (String containText : bodyContains) {
                            assert responseBody.contains(containText) : 
                                "Expected response to contain '" + containText + "' but was: " + responseBody;
                        }
                    }
                    
                    // 验证响应体的精确匹配（这里简化处理，实际可能需要更复杂的JSON解析）
                    if (bodyEquals != null) {
                        for (Map.Entry<String, Object> entry : bodyEquals.entrySet()) {
                            // 简化处理，实际实现可能需要使用JsonPath或其他方式验证
                            String expectedValue = entry.getValue().toString();
                            assert responseBody.contains("\"" + entry.getKey() + "\":" + expectedValue) || 
                                   responseBody.contains("\"" + entry.getKey() + "\": " + expectedValue) : 
                                "Expected response to contain '" + entry.getKey() + ":" + expectedValue + "' but was: " + responseBody;
                        }
                    }
                });
        }
    }
} 