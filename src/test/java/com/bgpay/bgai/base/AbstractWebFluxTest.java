package com.bgpay.bgai.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testng.annotations.BeforeClass;
import reactor.core.publisher.Mono;

/**
 * WebFlux控制器测试的基类
 * 提供WebTestClient相关的通用测试功能
 */
@AutoConfigureWebTestClient
public abstract class AbstractWebFluxTest extends BaseTestNGSpringContextTests {

    @Autowired
    protected WebTestClient webTestClient;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    @BeforeClass(alwaysRun = true)
    public void setUpWebFlux() {
        log.info("初始化WebFlux测试: {}", getClass().getSimpleName());
    }
    
    /**
     * 执行GET请求
     */
    protected WebTestClient.ResponseSpec performGet(String path) {
        log.info("执行GET请求: {}", path);
        Allure.step("执行GET请求: " + path);
        
        WebTestClient.ResponseSpec responseSpec = webTestClient.get()
                .uri(path)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
        
        // 记录请求和响应
        responseSpec.expectBody(String.class)
                .consumeWith(result -> addRequestResponseToAllure("GET", path, null, result));
        
        return webTestClient.get()
                .uri(path)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
    }
    
    /**
     * 执行带参数的GET请求
     */
    protected WebTestClient.ResponseSpec performGet(String path, Object... uriVars) {
        log.info("执行GET请求: {} 带参数: {}", path, uriVars);
        Allure.step("执行GET请求: " + path + " 带参数");
        
        WebTestClient.ResponseSpec responseSpec = webTestClient.get()
                .uri(path, uriVars)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
        
        // 记录请求和响应
        responseSpec.expectBody(String.class)
                .consumeWith(result -> addRequestResponseToAllure("GET", path, null, result));
        
        return webTestClient.get()
                .uri(path, uriVars)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
    }
    
    /**
     * 执行POST请求
     */
    protected WebTestClient.ResponseSpec performPost(String path, Object body) {
        log.info("执行POST请求: {}", path);
        Allure.step("执行POST请求: " + path);
        
        WebTestClient.ResponseSpec responseSpec = webTestClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(body), body.getClass())
                .exchange();
        
        // 记录请求和响应
        try {
            String requestBody = objectMapper.writeValueAsString(body);
            responseSpec.expectBody(String.class)
                    .consumeWith(result -> addRequestResponseToAllure("POST", path, requestBody, result));
        } catch (Exception e) {
            log.error("序列化请求体失败", e);
        }
        
        return webTestClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(body), body.getClass())
                .exchange();
    }
    
    /**
     * 执行PUT请求
     */
    protected WebTestClient.ResponseSpec performPut(String path, Object body) {
        log.info("执行PUT请求: {}", path);
        Allure.step("执行PUT请求: " + path);
        
        WebTestClient.ResponseSpec responseSpec = webTestClient.put()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(body), body.getClass())
                .exchange();
        
        // 记录请求和响应
        try {
            String requestBody = objectMapper.writeValueAsString(body);
            responseSpec.expectBody(String.class)
                    .consumeWith(result -> addRequestResponseToAllure("PUT", path, requestBody, result));
        } catch (Exception e) {
            log.error("序列化请求体失败", e);
        }
        
        return webTestClient.put()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(body), body.getClass())
                .exchange();
    }
    
    /**
     * 执行DELETE请求
     */
    protected WebTestClient.ResponseSpec performDelete(String path) {
        log.info("执行DELETE请求: {}", path);
        Allure.step("执行DELETE请求: " + path);
        
        WebTestClient.ResponseSpec responseSpec = webTestClient.delete()
                .uri(path)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
        
        // 记录请求和响应
        responseSpec.expectBody(String.class)
                .consumeWith(result -> addRequestResponseToAllure("DELETE", path, null, result));
        
        return webTestClient.delete()
                .uri(path)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
    }
    
    /**
     * 将请求和响应添加到Allure报告
     */
    private <T> void addRequestResponseToAllure(String method, String path, String requestBody, 
                                             EntityExchangeResult<T> result) {
        try {
            StringBuilder requestInfo = new StringBuilder();
            requestInfo.append(method).append(" ").append(path).append("\n");
            requestInfo.append("Content-Type: ").append(MediaType.APPLICATION_JSON).append("\n");
            
            if (requestBody != null && !requestBody.isEmpty()) {
                requestInfo.append("\n").append(requestBody);
            }
            
            Allure.addAttachment("HTTP Request", "text/plain", requestInfo.toString());
            
            StringBuilder responseInfo = new StringBuilder();
            responseInfo.append("Status: ").append(result.getStatus().value()).append("\n");
            responseInfo.append("Content-Type: ").append(MediaType.APPLICATION_JSON).append("\n");
            
            if (result.getResponseBody() != null) {
                responseInfo.append("\n").append(result.getResponseBody());
            }
            
            Allure.addAttachment("HTTP Response", "text/plain", responseInfo.toString());
            
        } catch (Exception e) {
            log.error("添加Allure附件失败", e);
        }
    }
} 