package com.bgpay.bgai.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testng.annotations.BeforeClass;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * MVC控制器测试的基类
 * 提供MockMvc相关的通用测试功能
 */
@WebMvcTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "seata.enabled=false",
    "saga.enabled=false"
})
public abstract class AbstractWebMvcTest extends BaseTestNGSpringContextTests {

    @Autowired
    protected MockMvc mockMvc;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    @BeforeClass(alwaysRun = true)
    public void setUpMvc() {
        log.info("初始化MVC测试: {}", getClass().getSimpleName());
    }
    
    /**
     * 执行GET请求
     */
    protected ResultActions performGet(String path) throws Exception {
        log.info("执行GET请求: {}", path);
        Allure.step("执行GET请求: " + path);
        
        MvcResult result = mockMvc.perform(get(path)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andReturn();
        
        addRequestResponseToAllure("GET", path, null, result);
        return mockMvc.perform(get(path).contentType(MediaType.APPLICATION_JSON));
    }
    
    /**
     * 执行带参数的GET请求
     */
    protected ResultActions performGet(String path, Object... uriVars) throws Exception {
        log.info("执行GET请求: {} 带参数: {}", path, uriVars);
        Allure.step("执行GET请求: " + path + " 带参数");
        
        MvcResult result = mockMvc.perform(get(path, uriVars)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andReturn();
        
        addRequestResponseToAllure("GET", path, null, result);
        return mockMvc.perform(get(path, uriVars).contentType(MediaType.APPLICATION_JSON));
    }
    
    /**
     * 执行POST请求
     */
    protected ResultActions performPost(String path, Object body) throws Exception {
        log.info("执行POST请求: {}", path);
        Allure.step("执行POST请求: " + path);
        
        String content = objectMapper.writeValueAsString(body);
        
        MvcResult result = mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
                .andDo(print())
                .andReturn();
        
        addRequestResponseToAllure("POST", path, content, result);
        return mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content));
    }
    
    /**
     * 执行PUT请求
     */
    protected ResultActions performPut(String path, Object body) throws Exception {
        log.info("执行PUT请求: {}", path);
        Allure.step("执行PUT请求: " + path);
        
        String content = objectMapper.writeValueAsString(body);
        
        MvcResult result = mockMvc.perform(put(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
                .andDo(print())
                .andReturn();
        
        addRequestResponseToAllure("PUT", path, content, result);
        return mockMvc.perform(put(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content));
    }
    
    /**
     * 执行DELETE请求
     */
    protected ResultActions performDelete(String path) throws Exception {
        log.info("执行DELETE请求: {}", path);
        Allure.step("执行DELETE请求: " + path);
        
        MvcResult result = mockMvc.perform(delete(path)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andReturn();
        
        addRequestResponseToAllure("DELETE", path, null, result);
        return mockMvc.perform(delete(path).contentType(MediaType.APPLICATION_JSON));
    }
    
    /**
     * 执行自定义请求
     */
    protected ResultActions performRequest(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        log.info("执行自定义请求");
        Allure.step("执行自定义请求");
        
        return mockMvc.perform(requestBuilder).andDo(print());
    }
    
    /**
     * 将请求和响应添加到Allure报告
     */
    private void addRequestResponseToAllure(String method, String path, String requestBody, MvcResult result) {
        try {
            String responseBody = result.getResponse().getContentAsString();
            
            StringBuilder requestInfo = new StringBuilder();
            requestInfo.append(method).append(" ").append(path).append("\n");
            requestInfo.append("Content-Type: ").append(MediaType.APPLICATION_JSON).append("\n");
            
            if (requestBody != null && !requestBody.isEmpty()) {
                requestInfo.append("\n").append(requestBody);
            }
            
            Allure.addAttachment("HTTP Request", "text/plain", requestInfo.toString());
            
            StringBuilder responseInfo = new StringBuilder();
            responseInfo.append("Status: ").append(result.getResponse().getStatus()).append("\n");
            responseInfo.append("Content-Type: ").append(result.getResponse().getContentType()).append("\n");
            responseInfo.append("\n").append(responseBody);
            
            Allure.addAttachment("HTTP Response", "text/plain", responseInfo.toString());
            
        } catch (Exception e) {
            log.error("添加Allure附件失败", e);
        }
    }
} 