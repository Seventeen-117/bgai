package com.bgpay.bgai.controller;

import com.bgpay.bgai.base.AbstractWebFluxTest;
import io.qameta.allure.*;
import org.springframework.http.MediaType;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * 示例WebFlux控制器测试类
 * 展示如何使用测试框架进行响应式控制器测试
 */
@Epic("API测试")
@Feature("响应式控制器")
@Owner("测试团队")
public class SampleWebFluxControllerTest extends AbstractWebFluxTest {

    /**
     * 测试获取消息列表接口
     */
    @Test(groups = {"controller-tests", "reactive-api"})
    @Story("消息管理")
    @Description("测试获取消息列表接口，验证返回状态码和响应格式")
    @Severity(SeverityLevel.CRITICAL)
    @Issue("BGAI-456")
    @TmsLink("TC-789")
    public void testGetMessages() {
        logStep("发送GET请求到/api/messages");
        
        performGet("/api/messages")
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.data").isArray()
            .jsonPath("$.success").isEqualTo(true);
        
        logStep("验证响应成功");
    }
    
    /**
     * 测试获取单个消息接口
     */
    @Test(groups = {"controller-tests", "reactive-api"})
    @Story("消息管理")
    @Description("测试获取单个消息接口，验证返回的消息信息正确")
    @Severity(SeverityLevel.NORMAL)
    public void testGetMessageById() {
        String messageId = "456";
        logStep("发送GET请求到/api/messages/{id}");
        
        performGet("/api/messages/{id}", messageId)
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.data.id").isEqualTo(messageId)
            .jsonPath("$.data.content").isNotEmpty()
            .jsonPath("$.success").isEqualTo(true);
        
        logStep("验证消息信息正确");
    }
    
    /**
     * 测试创建消息接口
     */
    @Test(groups = {"controller-tests", "reactive-api"})
    @Story("消息管理")
    @Description("测试创建消息接口，验证消息创建成功")
    @Severity(SeverityLevel.CRITICAL)
    public void testCreateMessage() {
        // 创建测试数据
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("content", "测试消息内容");
        requestBody.put("sender", "测试用户");
        requestBody.put("timestamp", System.currentTimeMillis());
        
        logStep("发送POST请求到/api/messages");
        
        performPost("/api/messages", requestBody)
            .expectStatus().isCreated()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.data.id").isNotEmpty()
            .jsonPath("$.data.content").isEqualTo("测试消息内容")
            .jsonPath("$.data.sender").isEqualTo("测试用户")
            .jsonPath("$.success").isEqualTo(true);
        
        logStep("验证消息创建成功");
    }
    
    /**
     * 测试更新消息接口
     */
    @Test(groups = {"controller-tests", "reactive-api"})
    @Story("消息管理")
    @Description("测试更新消息接口，验证消息信息更新成功")
    @Severity(SeverityLevel.NORMAL)
    public void testUpdateMessage() {
        String messageId = "456";
        
        // 创建测试数据
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("content", "更新后的消息内容");
        requestBody.put("sender", "测试用户");
        requestBody.put("timestamp", System.currentTimeMillis());
        
        logStep("发送PUT请求到/api/messages/{id}");
        
        performPut("/api/messages/" + messageId, requestBody)
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.data.id").isEqualTo(messageId)
            .jsonPath("$.data.content").isEqualTo("更新后的消息内容")
            .jsonPath("$.success").isEqualTo(true);
        
        logStep("验证消息信息更新成功");
    }
    
    /**
     * 测试删除消息接口
     */
    @Test(groups = {"controller-tests", "reactive-api"})
    @Story("消息管理")
    @Description("测试删除消息接口，验证消息删除成功")
    @Severity(SeverityLevel.NORMAL)
    public void testDeleteMessage() {
        String messageId = "456";
        
        logStep("发送DELETE请求到/api/messages/{id}");
        
        performDelete("/api/messages/" + messageId)
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.success").isEqualTo(true);
        
        logStep("验证消息删除成功");
        
        // 验证消息已被删除
        performGet("/api/messages/{id}", messageId)
            .expectStatus().isNotFound();
        
        logStep("验证消息不存在");
    }
    
    /**
     * 测试消息流接口
     */
    @Test(groups = {"controller-tests", "reactive-api", "stream-api"})
    @Story("消息流")
    @Description("测试消息流接口，验证能够正确接收消息流")
    @Severity(SeverityLevel.CRITICAL)
    public void testMessageStream() {
        logStep("发送GET请求到/api/messages/stream");
        
        webTestClient.get()
            .uri("/api/messages/stream")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.TEXT_EVENT_STREAM)
            .returnResult(String.class)
            .getResponseBody()
            .take(5) // 只获取前5条消息
            .doOnNext(message -> {
                log.info("收到消息: {}", message);
                addAttachment("Stream Message", message);
            })
            .blockLast(); // 阻塞直到收到最后一条消息
        
        logStep("验证消息流接收成功");
    }
} 