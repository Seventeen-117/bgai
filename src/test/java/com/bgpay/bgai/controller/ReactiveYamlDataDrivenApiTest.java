package com.bgpay.bgai.controller;

import com.bgpay.bgai.controller.FeignDemoController;
import com.bgpay.bgai.controller.OpenApiController;
import com.bgpay.bgai.controller.ServiceDiscoveryExampleController;

import com.bgpay.bgai.base.YamlDataProvider;
import com.bgpay.bgai.base.YamlSource;
import com.bgpay.bgai.config.CircuitBreakerAutoConfigurationDisabler;
import com.bgpay.bgai.config.CacheWarmerMockConfig;
import com.bgpay.bgai.config.CircuitBreakerTestConfig;
import com.bgpay.bgai.config.CloudDiscoveryAutoConfigurationDisabler;
import com.bgpay.bgai.config.DataSourceAutoConfigurationDisabler;
import com.bgpay.bgai.config.DeepSeekServiceMockConfig;
import com.bgpay.bgai.config.FileTypeServiceMockConfig;
import com.bgpay.bgai.config.FeignAutoConfigurationDisabler;
import com.bgpay.bgai.config.RedisAutoConfigurationDisabler;
import com.bgpay.bgai.config.LoadBalancerAutoConfigurationDisabler;
import com.bgpay.bgai.config.LoadBalancerTestConfig;
import com.bgpay.bgai.config.MockEnvironmentConfig;
import com.bgpay.bgai.config.MockWebClientConfig;
import com.bgpay.bgai.config.ReactiveLoadBalancerConfig;
import com.bgpay.bgai.config.ReactiveChatControllerMockConfig;
import com.bgpay.bgai.config.RocketMQTestConfig;
import com.bgpay.bgai.config.ServiceDiscoveryMockConfig;
import com.bgpay.bgai.config.TestWebFluxConfig;
import com.bgpay.bgai.config.ValidationAutoConfigurationDisabler;
import com.bgpay.bgai.config.WebClientAutoConfigurationDisabler;
import com.bgpay.bgai.config.ApplicationTestConfig;
import com.bgpay.bgai.config.MockListenerContainerConfig;
import com.bgpay.bgai.config.MockExtConsumerResetConfig;
import com.bgpay.bgai.config.TestApplicationEnvironmentConfig;
import com.bgpay.bgai.config.SpringTestConfig;
import com.bgpay.bgai.config.UnifiedEnvironmentConfig;
import com.bgpay.bgai.config.UserServiceMockConfig;
import com.bgpay.bgai.config.ApiKeyServiceMockConfig;
import com.bgpay.bgai.config.BGAIServiceMockConfig;
import com.bgpay.bgai.config.ApiConfigServiceMockConfig;
import com.bgpay.bgai.config.WebClientTestOverrideConfig;
import com.bgpay.bgai.config.ExcludeMainWebClientConfig;
import com.bgpay.bgai.config.TransactionLogServiceMockConfig;
import com.bgpay.bgai.config.RocketMQBillingServiceMockConfig;
import com.bgpay.bgai.config.TestBeanPostProcessor;
import com.bgpay.bgai.config.DirectRocketMQBillingServiceProvider;
import com.bgpay.bgai.config.UsageControllerMockConfig;
import com.bgpay.bgai.config.BillingServiceMockConfig;
import com.bgpay.bgai.config.ChatRecordRepositoryMockConfig;
import com.bgpay.bgai.config.ChatWebFilterMockConfig;
import com.bgpay.bgai.config.ElasticsearchAutoConfigurationDisabler;
import com.bgpay.bgai.config.ElasticsearchMockConfig;
import com.bgpay.bgai.config.DeepSeekWebClientMockConfig;
import com.bgpay.bgai.config.WebClientBuilderUnifier;
import com.bgpay.bgai.config.WebClientConfigMock;
import com.bgpay.bgai.config.WebClientBeanExclusionConfig;
import com.bgpay.bgai.config.ReactiveWebMockConfig;
import com.bgpay.bgai.config.TestPropertySourceConfig;
import com.bgpay.bgai.config.RedisTemplateQualifierConfig;
import com.bgpay.bgai.config.WebClientCircularDependencyResolver;
import com.bgpay.bgai.config.WebClientConfigExcluder;
import com.bgpay.bgai.config.GatewayRouteMockConfig;
import com.bgpay.bgai.config.GatewayRouteConfigMock;
import com.bgpay.bgai.config.WebClientQualifierResolver;
import com.bgpay.bgai.entity.ApiConfig;
import com.bgpay.bgai.response.ChatResponse;
import com.bgpay.bgai.service.ApiConfigService;
import com.bgpay.bgai.service.ApiKeyService;
import com.bgpay.bgai.service.BGAIService;
import com.bgpay.bgai.service.UserService;
import com.bgpay.bgai.service.deepseek.DeepSeekService;
import com.bgpay.bgai.service.impl.FallbackService;
import com.bgpay.bgai.transaction.TransactionCoordinator;
import com.bgpay.bgai.utils.ServiceDiscoveryUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import reactor.core.publisher.Mono;
import org.springframework.test.context.ContextConfiguration;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 基于YAML数据驱动的WebFlux API测试示例
 */
@Feature("WebFlux API测试")
@WebFluxTest(
    controllers = {OpenApiController.class, ServiceDiscoveryExampleController.class, FeignDemoController.class}, 
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration.class,
        org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class,
        org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration.class,
        org.apache.rocketmq.spring.autoconfigure.ListenerContainerConfiguration.class,
        org.apache.rocketmq.spring.autoconfigure.RocketMQTransactionConfiguration.class,
        org.apache.rocketmq.spring.autoconfigure.ExtConsumerResetConfiguration.class,
        org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRepositoriesAutoConfiguration.class
    }
)
@AutoConfigureWebTestClient
@ContextConfiguration(initializers = SpringTestConfig.class)
@Import({
    WebClientCircularDependencyResolver.class,  // 解决WebClient循环依赖问题，最高优先级
    WebClientConfigExcluder.class,  // 排除主应用中的WebClientConfig
    WebClientQualifierResolver.class,  // 解决WebClient多主键冲突问题
    GatewayRouteMockConfig.class,  // 提供Gateway路由组件的Mock实现
    GatewayRouteConfigMock.class,  // 禁用主应用中的Gateway路由配置
    TestPropertySourceConfig.class,  // 添加测试配置文件
    UnifiedEnvironmentConfig.class,  // 提供统一的Environment配置，解决环境bean冲突
    TestWebFluxConfig.class, 
    MockWebClientConfig.class, 
    WebClientBuilderUnifier.class,  // 提供统一的WebClient.Builder实现，避免冲突
    WebClientTestOverrideConfig.class,  // 使用高优先级配置覆盖WebClientConfig
    WebClientAutoConfigurationDisabler.class,
    ExcludeMainWebClientConfig.class,  // 明确排除主应用中的WebClientConfig
    WebClientBeanExclusionConfig.class,  // 明确替换WebClientConfig bean
    DeepSeekWebClientMockConfig.class,  // 为DeepSeekServiceImp提供WebClient
    WebClientConfigMock.class,  // 提供完整的WebClientConfig替代实现
    ReactiveWebMockConfig.class,  // 提供反应式Web组件
    LoadBalancerTestConfig.class,
    LoadBalancerAutoConfigurationDisabler.class,
    ReactiveLoadBalancerConfig.class,
    CircuitBreakerTestConfig.class,
    CircuitBreakerAutoConfigurationDisabler.class,
    ReactiveChatControllerMockConfig.class,
    CloudDiscoveryAutoConfigurationDisabler.class,
    ServiceDiscoveryMockConfig.class,
    DeepSeekServiceMockConfig.class,  // 提供DeepSeekService和DeepSeekServiceImp的mock实现
    ValidationAutoConfigurationDisabler.class,
    ElasticsearchAutoConfigurationDisabler.class,  // 禁用Elasticsearch自动配置
    ElasticsearchMockConfig.class,  // 提供Elasticsearch组件的mock实现
    MockEnvironmentConfig.class,
    RocketMQTestConfig.class,  // 提供ConfigurableEnvironment并禁用RocketMQ相关配置
    ApplicationTestConfig.class,  // 使用系统属性禁用自动配置
    MockListenerContainerConfig.class,  // 提供模拟的ListenerContainerConfiguration
    MockExtConsumerResetConfig.class,  // 提供模拟的ExtConsumerResetConfiguration
    TestApplicationEnvironmentConfig.class,  // 提供标准的ConfigurableEnvironment
    SpringTestConfig.class,
    UserServiceMockConfig.class,  // 提供UserService mock bean
    ApiKeyServiceMockConfig.class,  // 提供ApiKeyService mock bean
    BGAIServiceMockConfig.class,  // 提供BGAIService mock bean
    ApiConfigServiceMockConfig.class,  // 提供ApiConfigService mock bean
    TransactionLogServiceMockConfig.class,  // 添加TransactionLogService mock配置
    RocketMQBillingServiceMockConfig.class,  // 添加RocketMQBillingService mock配置
    TestBeanPostProcessor.class,
    DirectRocketMQBillingServiceProvider.class,
    UsageControllerMockConfig.class,  // 添加UsageController的mock配置，解决BillingService冲突
    BillingServiceMockConfig.class,  // 添加BillingService的主要实现，解决多个@Primary冲突
    ChatRecordRepositoryMockConfig.class,  // 添加ChatRecordRepository mock配置
    ChatWebFilterMockConfig.class,  // 添加ChatRecordWebFilter mock配置
    DataSourceAutoConfigurationDisabler.class,  // 禁用主应用的数据源配置，使用测试专用数据源
    FileTypeServiceMockConfig.class,  // 提供FileTypeService和FileTypeMapper的Mock实现
    CacheWarmerMockConfig.class,  // 提供CacheWarmer的Mock实现
    RedisAutoConfigurationDisabler.class,  // 禁用主应用的Redis配置，使用测试专用Redis配置
    FeignAutoConfigurationDisabler.class,  // 禁用OpenFeign自动配置，使用Mock实现
    RedisTemplateQualifierConfig.class  // 添加RedisTemplate冲突解决配置
})
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "seata.enabled=false",
    "saga.enabled=false",
    "seata.saga.state-machine.auto-register=false",
    "spring.cloud.loadbalancer.enabled=false",
    "spring.cloud.discovery.client.simple.instances.service-id[0].uri=http://localhost:8080",
    "spring.cloud.loadbalancer.ribbon.enabled=false",
    "spring.cloud.service-registry.auto-registration.enabled=false",
    "eureka.client.enabled=false",
    "spring.cloud.config.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "spring.cloud.consul.enabled=false",
    "spring.cloud.zookeeper.enabled=false",
    "spring.cloud.gateway.enabled=false",
    "spring.cloud.circuitbreaker.resilience4j.enabled=false",
    "spring.cloud.circuitbreaker.enabled=false",
    "spring.validation.is-method-validation-bean-post-processor-enabled=false",
    "rocketmq.messageConsumer.enabled=false",
    "rocketmq.producer.enable=false",
    "rocketmq.name-server=8.133.246.113:9876",
    "rocketmq.producer.group=test-group",
    "spring.data.elasticsearch.repositories.enabled=false",
    "spring.elasticsearch.enabled=false",
    "spring.webflux.base-path=/api",
    "spring.main.web-application-type=reactive",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration"
})
public class ReactiveYamlDataDrivenApiTest extends AbstractTestNGSpringContextTests {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected String testId;
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // 模拟依赖的服务
    @MockBean
    private UserService userService;
    
    @MockBean
    private ApiKeyService apiKeyService;
    
    @MockBean
    private ApiConfigService apiConfigService;
    
    @MockBean
    private BGAIService bgaiService;
    
    // 使用@Autowired而不是@MockBean，因为DeepSeekServiceMockConfig已经提供了@Primary的bean
    @Autowired
    private DeepSeekService deepSeekService;
    
    @MockBean
    private TransactionCoordinator transactionCoordinator;
    
    @MockBean
    private FallbackService fallbackService;
    
    @MockBean
    private ServiceDiscoveryUtils serviceDiscoveryUtils;
    
    /**
     * 在每个测试方法执行前执行
     */
    @BeforeMethod(alwaysRun = true)
    public void setUpMethod(Method method) {
        testId = UUID.randomUUID().toString();
        log.info("开始执行测试方法: {}, testId: {}", method.getName(), testId);
        Allure.parameter("testId", testId);
        Allure.parameter("testMethod", method.getName());
        
        // 设置DeepSeekService的mock行为
        // 这里使用的是DeepSeekServiceMockConfig中提供的mock实现
        ChatResponse mockResponse = new ChatResponse();
        mockResponse.setContent("Mock response content");
        mockResponse.setSuccess(true);
        
        Mockito.when(deepSeekService.processRequestReactive(
            Mockito.anyMap(), 
            Mockito.anyString(), 
            Mockito.anyString(), 
            Mockito.anyString(), 
            Mockito.anyString(), 
            Mockito.anyBoolean()
        )).thenReturn(Mono.just(mockResponse));
        
        // 设置ApiConfigService的mock行为
        ApiConfig mockConfig = new ApiConfig();
        mockConfig.setApiUrl("https://api.test.com");
        mockConfig.setApiKey("test-api-key");
        mockConfig.setModelName("test-model");
        
        Mockito.when(apiConfigService.findMatchingConfig(
            Mockito.anyString(), 
            Mockito.anyString(), 
            Mockito.anyString(), 
            Mockito.anyString()
        )).thenReturn(mockConfig);
    }
    
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
    
    /**
     * 记录测试步骤（用于Allure报告）
     */
    protected void logStep(String stepDescription) {
        log.info("测试步骤: {}", stepDescription);
        Allure.step(stepDescription);
    }
    
    /**
     * 添加测试附件（用于Allure报告）
     */
    protected void addAttachment(String name, String content) {
        log.debug("添加测试附件: {}", name);
        Allure.addAttachment(name, content);
    }
    
    /**
     * 添加测试附件（用于Allure报告）
     */
    protected void addAttachment(String name, String contentType, String content) {
        log.debug("添加测试附件: {}, 类型: {}", name, contentType);
        Allure.addAttachment(name, contentType, content);
    }
} 