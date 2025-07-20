package com.bgpay.bgai.controller;

import com.bgpay.bgai.base.YamlDataProvider;
import com.bgpay.bgai.base.YamlSource;
import com.bgpay.bgai.config.*;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
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
 * 使用YAML数据驱动的反应式API测试类
 * 
 * 这个类使用Spring Boot的WebFlux测试框架和TestNG进行测试
 * 使用YAML文件作为测试数据源
 */
@WebFluxTest(controllers = {
    ReactiveChatController.class
}, excludeAutoConfiguration = {
    org.springframework.cloud.gateway.config.GatewayAutoConfiguration.class,
    org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration.class,
    org.springframework.cloud.gateway.config.GatewayMetricsAutoConfiguration.class,
    org.springframework.cloud.gateway.config.GatewayReactiveLoadBalancerClientAutoConfiguration.class
}, excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        com.bgpay.bgai.config.GatewayRouteConfig.class,
        org.springframework.cloud.gateway.route.RouteLocator.class,
        org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder.class
    })
})
@AutoConfigureWebTestClient
@TestPropertySource(locations = "classpath:application-test.yml", properties = {
    "spring.cloud.gateway.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.cloud.gateway.config.GatewayAutoConfiguration"
})
@Import({
    // Gateway相关配置，按优先级顺序导入
    TestGatewayRouteConfigProvider.class, // 最高优先级，提供唯一的testGatewayRouteConfig bean
    GatewayRouteConfigReplacement.class,  // 提供替代的GatewayRouteConfig实现
    TestGatewayRouteConfigDisabler.class,  // 禁用TestGatewayRouteConfig
    CompleteGatewayDisablingConfig.class,  // 综合Gateway禁用配置
    GatewayBeanFactoryPostProcessor.class,  // Gateway Bean工厂后处理器
    KeyResolverBeanPostProcessor.class,  // 专门处理KeyResolver相关的bean定义
    
    // 添加新的Mock配置
    AllMappersMockConfig.class,  // 提供所有Mapper接口的mock实现
    PriceConfigServiceImplMockConfig.class,  // 提供PriceConfigServiceImpl的mock实现
    PriceVersionServiceImplMockConfig.class,  // 提供PriceVersionServiceImpl的mock实现
    
    // 原有的Mock配置
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
    MockExtConsumerResetConfig.class,  // 提供模拟的ExtConsumerResetConfiguration
    TestApplicationEnvironmentConfig.class,  // 提供标准的ConfigurableEnvironment
    SpringTestConfig.class,
    UserServiceMockConfig.class,  // 提供UserService mock bean
    ApiKeyServiceMockConfig.class,  // 提供ApiKeyService mock bean
    ApiConfigServiceMockConfig.class,  // 提供ApiConfigService mock bean
    BGAIServiceMockConfig.class,  // 提供BGAIService mock bean
    UsageInfoServiceMockConfig.class,  // 提供UsageInfoService mock bean
    ChatCompletionsMapperMockConfig.class,  // 提供ChatCompletionsMapper的mock实现
    ChatCompletionsServiceImplMockConfig.class,  // 提供ChatCompletionsServiceImpl的mock实现
    ChoicesMapperMockConfig.class,  // 提供ChoicesMapper的mock实现
    ChoicesServiceImplMockConfig.class,  // 提供ChoicesServiceImpl的mock实现
    MyBatisMockConfig.class,  // 提供MyBatis相关组件的Mock实现
    TestComponentScanFilterRegistrar.class  // 注册组件扫描过滤器
})
public class ReactiveYamlDataDrivenApiTest extends AbstractTestNGSpringContextTests {
    
    private static final Logger log = LoggerFactory.getLogger(ReactiveYamlDataDrivenApiTest.class);
    
    private String testId;
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ApiKeyService apiKeyService;
    
    @Autowired
    private ApiConfigService apiConfigService;
    
    @Autowired
    private BGAIService bgaiService;
    
    @Autowired
    private DeepSeekService deepSeekService;
    
    @Autowired
    private TransactionCoordinator transactionCoordinator;
    
    /**
     * 在每个测试方法执行前执行
     */
    @BeforeMethod(alwaysRun = true)
    public void setUpMethod(Method method) {
        testId = UUID.randomUUID().toString();
        log.info("开始执行测试方法: {}, testId: {}", method.getName(), testId);
        Allure.parameter("testId", testId);
        Allure.parameter("testMethod", method.getName());
        
        // 不需要在这里设置mock行为，因为我们已经在各自的MockConfig类中设置了
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
        
        // 验证响应体包含指定内容
        if (bodyContains != null) {
            for (String content : bodyContains) {
                responseSpec.expectBody().consumeWith(response -> {
                    String responseBody = new String(response.getResponseBody());
                    assert responseBody.contains(content) : "Response body does not contain: " + content;
                });
            }
        }
        
        // 验证响应体完全匹配
        if (bodyEquals != null) {
            responseSpec.expectBody().json(objectMapper.valueToTree(bodyEquals).toString());
        }
    }
    
    /**
     * 使用YAML数据驱动测试用户API
     * 
     * @param testData YAML文件中的测试数据
     */
    @Test(dataProvider = "namedYamlData", dataProviderClass = YamlDataProvider.class)
    @YamlSource("api/reactive-users")
    @Description("测试用户API的各种场景")
    @Story("用户管理功能")
    public void testUserApi(Map<String, Object> testData) {
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
        
        // 验证响应体包含指定内容
        if (bodyContains != null) {
            for (String content : bodyContains) {
                responseSpec.expectBody().consumeWith(response -> {
                    String responseBody = new String(response.getResponseBody());
                    assert responseBody.contains(content) : "Response body does not contain: " + content;
                });
            }
        }
        
        // 验证响应体完全匹配
        if (bodyEquals != null) {
            responseSpec.expectBody().json(objectMapper.valueToTree(bodyEquals).toString());
        }
    }
} 