package com.bgpay.bgai.controller;

import com.bgpay.bgai.base.YamlDataProvider;
import com.bgpay.bgai.base.YamlSource;
import com.bgpay.bgai.config.*;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import com.bgpay.bgai.service.ApiConfigService;
import com.bgpay.bgai.service.ApiKeyService;
import com.bgpay.bgai.service.BGAIService;
import com.bgpay.bgai.service.TransactionLogService;
import com.bgpay.bgai.service.UserService;
import com.bgpay.bgai.service.deepseek.DeepSeekService;
import com.bgpay.bgai.service.deepseek.FileProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 使用YAML数据驱动的API测试类
 * 
 * 这个类使用Spring Boot的测试框架和TestNG进行测试
 * 使用YAML文件作为测试数据源
 */
@SpringBootTest(
    classes = {
        AuthController.class,
        ApiKeyController.class,
        DynamicRouteController.class,
        SystemConfigController.class,
        GatewayRouteConfigReplacement.class,
        DeepSeekServiceMockConfig.class,
        ApiKeyServiceMockConfig.class,
        ApiConfigServiceMockConfig.class,
        UserServiceMockConfig.class,
        BGAIServiceMockConfig.class,
        TransactionLogServiceMockConfig.class,
        UsageInfoServiceMockConfig.class,
        SystemConfigControllerMockConfig.class,
        DynamicRouteServiceMockConfig.class,
        AllMappersMockConfig.class,
        ChatCompletionsServiceImplMockConfig.class,
        ChoicesServiceImplMockConfig.class,
        PriceConfigServiceImplMockConfig.class,
        PriceVersionServiceImplMockConfig.class,
        DeepSeekWebClientMockConfig.class,
        WebClientConfigMock.class,
        WebClientConfigOverrideConfig.class,
        ReactiveWebMockConfig.class,
        TestPropertySourceConfig.class,
        EnhancedChatControllerMockConfig.class,
        ChatWebFilterMockConfig.class,
        ChatRecordRepositoryMockConfig.class,
        ElasticsearchMockConfig.class,
        ElasticsearchAutoConfigurationDisabler.class,
        DataSourceAutoConfigurationDisabler.class,
        FileTypeServiceMockConfig.class,
        CacheWarmerMockConfig.class,
        RedisAutoConfigurationDisabler.class,
        RedisMockConfig.class,
        FeignAutoConfigurationDisabler.class,
        RedisTemplateQualifierConfig.class,
        TransactionCoordinatorMockConfig.class,
        ChatCompletionsServiceMockConfig.class,
        SagaStateMachineMockConfig.class,
        MyBatisMockConfig.class,
        TestComponentScanFilterRegistrar.class,
        UsageRecordServiceMockConfig.class,
        PriceCacheServiceMockConfig.class,
        FileWriterServiceMockConfig.class,
        AsyncTaskExecutorMockConfig.class,
        RocketMQProducerServiceMockConfig.class,
        RocketMQTemplateMockConfig.class,
        FallbackServiceMockConfig.class,
        MockMvcConfig.class
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ComponentScan(
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {com.bgpay.bgai.service.deepseek.DeepSeekServiceImp.class}
    )
)
@TestPropertySource(locations = "classpath:application-test.yml", properties = {
    "spring.cloud.gateway.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.cloud.gateway.config.GatewayAutoConfiguration"
})
@Feature("API测试")
public class YamlDataDrivenApiTest extends AbstractTestNGSpringContextTests {
    
    private static final Logger log = LoggerFactory.getLogger(YamlDataDrivenApiTest.class);
    
    private String testId;
    
    @Autowired
    private MockMvc mockMvc;
    
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
    
    // 使用@Autowired代替@MockBean，因为DeepSeekServiceMockConfig已经提供了bean
    @Autowired
    private DeepSeekService deepSeekService;
    
    @Autowired
    private FileProcessor fileProcessor;
    
    @Autowired
    private TransactionLogService transactionLogService;
    
    @MockBean(name = "customRedisRateLimiter")
    private Object customRedisRateLimiter;

    @MockBean(name = "gatewayRouteConfig")
    private Object gatewayRouteConfig;
    
    @MockBean(name = "nacosWarningHandler")
    private Object nacosWarningHandler;
    
    /**
     * 在每个测试方法执行前执行
     */
    @BeforeMethod(alwaysRun = true)
    public void setUpMethod(Method method) {
        testId = UUID.randomUUID().toString();
        log.info("开始执行测试方法: {}, testId: {}", method.getName(), testId);
        Allure.parameter("testId", testId);
        Allure.parameter("testMethod", method.getName());
    }
    
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
                requestBuilder = requestBuilder.header(header.getKey(), header.getValue().toString());
            }
        }
        
        // 添加请求体
        if (body != null) {
            requestBuilder = requestBuilder
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body));
        }
        
        // 执行请求
        ResultActions resultActions = mockMvc.perform(requestBuilder);
        
        // 验证状态码
        resultActions = resultActions.andExpect(MockMvcResultMatchers.status().is(statusCode));
        
        // 验证响应体包含指定内容
        if (bodyContains != null) {
            for (String content : bodyContains) {
                resultActions = resultActions.andExpect(MockMvcResultMatchers.content().string(Matchers.containsString(content)));
            }
        }
        
        // 验证响应体完全匹配
        if (bodyEquals != null) {
            resultActions = resultActions.andExpect(MockMvcResultMatchers.content().json(objectMapper.writeValueAsString(bodyEquals)));
        }
    }
    
    /**
     * 使用YAML数据驱动测试用户API
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
                requestBuilder = requestBuilder.header(header.getKey(), header.getValue().toString());
            }
        }
        
        // 添加请求体
        if (body != null) {
            requestBuilder = requestBuilder
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body));
        }
        
        // 执行请求
        ResultActions resultActions = mockMvc.perform(requestBuilder);
        
        // 验证状态码
        resultActions = resultActions.andExpect(MockMvcResultMatchers.status().is(statusCode));
        
        // 验证响应体包含指定内容
        if (bodyContains != null) {
            for (String content : bodyContains) {
                resultActions = resultActions.andExpect(MockMvcResultMatchers.content().string(Matchers.containsString(content)));
            }
        }
        
        // 验证响应体完全匹配
        if (bodyEquals != null) {
            resultActions = resultActions.andExpect(MockMvcResultMatchers.content().json(objectMapper.writeValueAsString(bodyEquals)));
        }
    }
} 