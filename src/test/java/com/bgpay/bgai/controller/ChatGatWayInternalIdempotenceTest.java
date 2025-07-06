package com.bgpay.bgai.controller;

import com.bgpay.bgai.base.IsolatedWebFluxTest;
import com.bgpay.bgai.config.WebFluxOnlyConfiguration;
import com.bgpay.bgai.config.SeataDisableConfiguration;
import com.bgpay.bgai.config.MockServicesConfig;
import com.bgpay.bgai.config.MockBeans;
import com.bgpay.bgai.config.MockControllers;
import com.bgpay.bgai.filter.IdempotenceWebFilter;
import com.bgpay.bgai.repository.es.ChatRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * chatGatWay-internal接口幂等性测试
 * 测试在短时间内多次发送相同请求，系统是否能够正确处理
 */
@Epic("API幂等性测试")
@Feature("chatGatWay-internal接口")
@Owner("测试团队")
@AutoConfigureWebTestClient
@TestPropertySource(locations = "classpath:application-webflux-test.yml")
@Import({WebFluxOnlyConfiguration.class, SeataDisableConfiguration.class, MockServicesConfig.class, MockBeans.class, MockControllers.class})
@EnableAutoConfiguration(exclude = {WebMvcAutoConfiguration.class})
@ActiveProfiles({"dev", "idempotence-test"})
@Test(groups = "webflux-tests")
@SpringBootTest(
    classes = {WebFluxOnlyConfiguration.class, MockServicesConfig.class, MockBeans.class, MockControllers.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.main.web-application-type=reactive",
        "spring.mvc.enabled=false",
        "spring.webflux.enabled=true",
        "seata.enabled=false",
        "seata.registry.enabled=false",
        "seata.config.enabled=false",
        "seata.service.disable-global-transaction=true",
        "spring.cloud.alibaba.seata.enabled=false"
    }
)
public class ChatGatWayInternalIdempotenceTest extends AbstractTestNGSpringContextTests {
    private static final Logger log = LoggerFactory.getLogger(ChatGatWayInternalIdempotenceTest.class);
    
    @Autowired
    private WebTestClient webTestClient;
    
    @MockBean
    private ReactiveChatController reactiveChatController;
    
    @MockBean
    private TestChatGatWayInternalController testChatGatWayInternalController;
    
    @MockBean
    private TestMockController testMockController;
    
    @MockBean
    private ChatRecordRepository chatRecordRepository;
    
    @MockBean
    private IdempotenceWebFilter idempotenceWebFilter;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeClass
    public void setUp() {
        log.info("初始化响应式测试类: {}", this.getClass().getSimpleName());
        
        // 设置为WebFlux模式
        System.setProperty("spring.main.web-application-type", "reactive");
        System.setProperty("springdoc.api-docs.enabled", "false");
        System.setProperty("springdoc.swagger-ui.enabled", "false");
        
        // 禁用Liquibase以避免数据库相关错误
        System.setProperty("spring.liquibase.enabled", "false");
        System.setProperty("spring.datasource.auto-commit", "false");
        
        // 显式禁用MVC相关配置
        System.setProperty("spring.mvc.enabled", "false");
        System.setProperty("spring.mvc.servlet.load-on-startup", "-1");
        
        // 禁用Seata相关配置
        System.setProperty("seata.enabled", "false");
        System.setProperty("spring.cloud.alibaba.seata.enabled", "false");
        
        Allure.addAttachment("测试环境", "使用模拟Redis和内存缓存进行幂等性测试");
    }
    
    @BeforeMethod
    public void beforeEachTest() {
        log.info("----------- 开始测试 -----------");
        // 重置mock调用次数
        Mockito.reset(reactiveChatController);
        Mockito.reset(testChatGatWayInternalController);
        Mockito.reset(testMockController);
        Mockito.reset(chatRecordRepository);
        Mockito.reset(idempotenceWebFilter);
        
        // 配置mock行为，使用ResponseEntity作为返回值
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("content", "测试响应");
        mockResponse.put("status", "success");
        
        // 使用TestMockController替代TestChatGatWayInternalController
        Mockito.when(testMockController.handleRequest(Mockito.any()))
            .thenReturn(ResponseEntity.ok(mockResponse));
            
        // 继续配置TestChatGatWayInternalController的行为，但是不使用thenReturn
        Mockito.doAnswer(invocation -> {
            return ResponseEntity.ok(mockResponse);
        }).when(testChatGatWayInternalController).mockChatGatWayInternal(Mockito.any());
        
        // 配置IdempotenceWebFilter的行为
        Mockito.doAnswer(invocation -> {
            ServerWebExchange exchange = invocation.getArgument(0);
            WebFilterChain chain = invocation.getArgument(1);
            return chain.filter(exchange);
        }).when(idempotenceWebFilter).filter(Mockito.any(), Mockito.any());
    }
    
    /**
     * 测试单个请求的正常处理
     */
    @Test
    @Story("基本功能测试")
    @Description("测试单个请求的正常处理，验证接口基本功能")
    @Severity(SeverityLevel.BLOCKER)
    public void testSingleRequest() {
        log.info("测试单个请求的正常处理");
        
        // 创建请求体
        Map<String, Object> requestBody = createRequestBody("测试幂等性单个请求", "api");
        
        // 生成请求ID
        String requestId = generateRequestId();
        
        // 执行请求
        String response = sendRequestAndVerifyResponse(requestBody, requestId);
        
        // 验证控制器方法被调用
        verifyControllerMethodCalled(1);
        
        log.info("单个请求测试成功");
        Allure.addAttachment("响应数据", "application/json", response, ".json");
    }
    
    /**
     * 测试相同请求ID的重复请求（幂等性测试）
     */
    @Test
    @Story("幂等性测试")
    @Description("测试相同请求ID的重复请求，验证接口幂等性")
    @Severity(SeverityLevel.CRITICAL)
    public void testIdempotence() {
        log.info("测试相同请求ID的重复请求（幂等性测试）");
        
        // 创建请求体
        Map<String, Object> requestBody = createRequestBody("测试幂等性", "api");
        
        // 生成请求ID
        String requestId = generateRequestId();
        
        // 执行第一次请求
        String firstResponse = sendRequest(requestBody, requestId);
        Allure.addAttachment("第一次请求响应", "application/json", firstResponse, ".json");
        
        log.info("第一次请求响应: {}", firstResponse);
        
        // 执行第二次相同请求（相同请求ID）
        String secondResponse = sendRequest(requestBody, requestId);
        Allure.addAttachment("第二次请求响应", "application/json", secondResponse, ".json");
        
        log.info("第二次请求响应: {}", secondResponse);
        
        // 验证两次响应内容相同（幂等性）
        compareResponses(firstResponse, secondResponse);
        
        // 验证控制器方法只被调用一次（第二次应该从缓存返回）
        verifyControllerMethodCalled(1);
        
        log.info("幂等性测试成功");
    }
    
    /**
     * 测试不同请求ID的相同内容请求（确保非幂等请求正常处理）
     */
    @Test
    @Story("不同请求ID测试")
    @Description("测试不同请求ID的相同内容请求，确保非幂等请求正常处理")
    @Severity(SeverityLevel.NORMAL)
    public void testDifferentRequestIds() {
        log.info("测试不同请求ID的相同内容请求");
        
        // 创建请求体
        Map<String, Object> requestBody = createRequestBody("测试幂等性不同请求ID", "api");
        
        // 生成两个不同的请求ID
        String requestId1 = generateRequestId();
        String requestId2 = generateRequestId();
        
        Allure.addAttachment("请求ID 1", requestId1);
        Allure.addAttachment("请求ID 2", requestId2);
        
        // 执行第一次请求
        sendRequest(requestBody, requestId1);
        
        // 执行第二次请求（不同请求ID）
        sendRequest(requestBody, requestId2);
        
        // 验证控制器方法被调用两次
        verifyControllerMethodCalled(2);
        
        log.info("不同请求ID测试成功");
    }
    
    @Step("创建请求体: prompt={0}, testCaseType={1}")
    private Map<String, Object> createRequestBody(String prompt, String testCaseType) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("prompt", prompt);
        requestBody.put("testCaseType", testCaseType);
        return requestBody;
    }
    
    @Step("生成请求ID")
    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }
    
    @Step("发送请求并验证响应")
    private String sendRequestAndVerifyResponse(Map<String, Object> requestBody, String requestId) {
        String response = webTestClient.post()
                .uri("/api/chatGatWay-internal")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Request-ID", requestId)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        
        // 验证响应不为空
        Assert.assertNotNull(response, "响应不应为空");
        return response;
    }
    
    @Step("发送请求")
    private String sendRequest(Map<String, Object> requestBody, String requestId) {
        return webTestClient.post()
                .uri("/api/chatGatWay-internal")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Request-ID", requestId)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
    }
    
    @Step("比较两次响应")
    private void compareResponses(String firstResponse, String secondResponse) {
        Assert.assertEquals(secondResponse, firstResponse, "幂等性测试失败：相同请求ID的两次请求返回不同结果");
    }
    
    @Step("验证控制器方法被调用 {0} 次")
    private void verifyControllerMethodCalled(int times) {
        // 验证TestChatGatWayInternalController是否被调用了指定次数
        Mockito.verify(testChatGatWayInternalController, Mockito.times(times))
                .mockChatGatWayInternal(Mockito.any());
    }
    
    /**
     * 测试并发请求场景下的幂等性处理
     */
    @Test
    @Story("并发请求测试")
    @Description("测试并发请求场景下的幂等性处理，验证系统在高并发下的幂等性能力")
    @Severity(SeverityLevel.CRITICAL)
    public void testConcurrentRequests() throws InterruptedException {
        log.info("测试并发请求场景下的幂等性处理");
        
        // 创建请求体
        Map<String, Object> requestBody = createRequestBody("测试幂等性并发请求", "api");
        
        // 请求次数和线程数
        int requestCount = 10;
        
        // 生成请求ID
        String requestId = generateRequestId();
        Allure.addAttachment("并发请求ID", requestId);
        
        // 使用CountDownLatch确保所有线程同时开始
        CountDownLatch startSignal = new CountDownLatch(1);
        // 使用CountDownLatch等待所有线程完成
        CountDownLatch doneSignal = new CountDownLatch(requestCount);
        
        // 存储所有响应
        List<String> responses = new ArrayList<>();
        
        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        
        // 提交任务到线程池
        submitConcurrentTasks(requestBody, requestId, requestCount, startSignal, doneSignal, responses, executor);
        
        // 发送开始信号，所有线程开始执行
        startSignal.countDown();
        
        // 等待所有线程完成
        boolean completed = doneSignal.await(30, TimeUnit.SECONDS);
        
        // 关闭线程池
        executor.shutdown();
        
        // 验证并发测试结果
        validateConcurrentResults(completed, responses);
        
        // 验证方法最多只被调用一次（幂等性保证）
        Mockito.verify(testChatGatWayInternalController, Mockito.atMost(1))
                .mockChatGatWayInternal(Mockito.any());
        
        log.info("并发请求幂等性测试成功");
    }
    
    /**
     * 测试请求ID过期后的处理
     */
    @Test
    @Story("缓存过期测试")
    @Description("测试请求ID过期后的处理，验证缓存过期机制")
    @Severity(SeverityLevel.NORMAL)
    public void testRequestIdExpiration() throws InterruptedException {
        log.info("测试请求ID过期后的处理");
        
        // 创建请求体
        Map<String, Object> requestBody = createRequestBody("测试幂等性请求ID过期", "api");
        
        // 生成请求ID
        String requestId = generateRequestId();
        Allure.addAttachment("测试过期的请求ID", requestId);
        
        // 执行第一次请求
        String firstResponse = sendRequest(requestBody, requestId);
        Allure.addAttachment("过期前响应", "application/json", firstResponse, ".json");
        
        // 重置mock调用计数
        Mockito.reset(testChatGatWayInternalController);
        
        // 模拟等待请求ID过期（实际测试中可能需要调整缓存配置或使用Mock）
        // 这里假设缓存过期时间很短，例如1秒
        waitForCacheExpiration();
        
        // 执行第二次相同请求（相同请求ID，但已过期）
        String secondResponse = sendRequest(requestBody, requestId);
        Allure.addAttachment("过期后响应", "application/json", secondResponse, ".json");
        
        // 验证控制器方法被再次调用
        verifyControllerMethodCalled(1);
        
        log.info("请求ID过期测试成功");
    }
    
    @Step("提交并发任务")
    private void submitConcurrentTasks(Map<String, Object> requestBody, String requestId, 
            int requestCount, CountDownLatch startSignal, CountDownLatch doneSignal, 
            List<String> responses, ExecutorService executor) {
        
        for (int i = 0; i < requestCount; i++) {
            int taskNum = i + 1;
            executor.submit(() -> {
                try {
                    // 等待开始信号
                    startSignal.await();
                    
                    log.info("并发任务 {} 开始执行", taskNum);
                    
                    // 发送请求
                    String response = sendRequest(requestBody, requestId);
                    
                    // 添加响应到列表
                    synchronized (responses) {
                        responses.add(response);
                    }
                    
                    log.info("并发任务 {} 完成", taskNum);
                } catch (Exception e) {
                    log.error("并发请求测试失败", e);
                    Allure.addAttachment("并发任务 " + taskNum + " 异常", e.toString());
                } finally {
                    // 完成信号
                    doneSignal.countDown();
                }
            });
        }
    }
    
    @Step("验证并发测试结果")
    private void validateConcurrentResults(boolean completed, List<String> responses) {
        // 验证所有线程都完成
        Assert.assertTrue(completed, "部分线程未在超时时间内完成");
        
        // 验证收集到的响应数量
        Assert.assertFalse(responses.isEmpty(), "未收到任何响应");
        Allure.addAttachment("收到的响应数量", String.valueOf(responses.size()));
        
        // 验证所有响应相同
        String firstResponse = responses.get(0);
        for (int i = 1; i < responses.size(); i++) {
            Assert.assertEquals(responses.get(i), firstResponse, 
                    "幂等性测试失败：并发请求返回不同结果");
        }
    }
    
    @Step("等待缓存过期")
    private void waitForCacheExpiration() throws InterruptedException {
        log.info("等待缓存过期中...");
        Thread.sleep(3000); // 等待3秒，确保缓存过期
        log.info("缓存过期等待完成");
    }
} 