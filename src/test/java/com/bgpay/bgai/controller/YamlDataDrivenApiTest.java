package com.bgpay.bgai.controller;

import com.bgpay.bgai.base.YamlDataProvider;
import com.bgpay.bgai.base.YamlSource;
import com.bgpay.bgai.config.*;
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

/**
 * 使用YAML数据驱动的API测试类
 * 
 * 这个类使用Spring Boot的测试框架和TestNG进行测试
 * 使用YAML文件作为测试数据源
 */
@WebMvcTest(controllers = {
    AuthController.class,
    ApiKeyController.class,
    DynamicRouteController.class,
    SystemConfigController.class
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@Feature("API测试")
@Import({
    DeepSeekServiceMockConfig.class,  // 提供DeepSeekService和DeepSeekServiceImp的mock实现
    ApiKeyServiceMockConfig.class,  // 提供ApiKeyService的mock实现
    ApiConfigServiceMockConfig.class,  // 提供ApiConfigService的mock实现
    UserServiceMockConfig.class,  // 提供UserService的mock实现
    BGAIServiceMockConfig.class,  // 提供BGAIService的mock实现
    TransactionLogServiceMockConfig.class,  // 提供TransactionLogService的mock实现
    UsageInfoServiceMockConfig.class,  // 提供UsageInfoService的mock实现
    SystemConfigControllerMockConfig.class,  // 提供SystemConfigController的mock实现
    DynamicRouteServiceMockConfig.class,  // 提供DynamicRouteService的mock实现
    AllMappersMockConfig.class,  // 提供所有Mapper接口的mock实现
    ChatCompletionsServiceImplMockConfig.class,  // 提供ChatCompletionsServiceImpl的mock实现
    ChoicesServiceImplMockConfig.class,  // 提供ChoicesServiceImpl的mock实现
    PriceConfigServiceImplMockConfig.class,  // 提供PriceConfigServiceImpl的mock实现
    PriceVersionServiceImplMockConfig.class,  // 提供PriceVersionServiceImpl的mock实现
    DeepSeekWebClientMockConfig.class,  // 为DeepSeekServiceImp提供WebClient
    WebClientConfigMock.class,  // 提供完整的WebClientConfig替代实现
    WebClientConfigOverrideConfig.class,  // 提供完整覆盖实现，直接替代WebClient bean
    ReactiveWebMockConfig.class,  // 提供反应式Web组件
    TestPropertySourceConfig.class,  // 添加测试配置文件
    EnhancedChatControllerMockConfig.class,
    ChatWebFilterMockConfig.class,
    ChatRecordRepositoryMockConfig.class,
    ElasticsearchMockConfig.class,
    ElasticsearchAutoConfigurationDisabler.class,
    RocketMQMockConfig.class,
    DataSourceAutoConfigurationDisabler.class,  // 禁用主应用的数据源配置，使用测试专用数据源
    FileTypeServiceMockConfig.class,  // 提供FileTypeService和FileTypeMapper的Mock实现
    CacheWarmerMockConfig.class,  // 提供CacheWarmer的Mock实现
    RedisAutoConfigurationDisabler.class,  // 禁用主应用的Redis配置，使用测试专用Redis配置
    RedisMockConfig.class,  // 提供RedisConfig的Mock实现，解决bean创建错误
    FeignAutoConfigurationDisabler.class,  // 禁用OpenFeign自动配置，使用Mock实现
    RedisTemplateQualifierConfig.class,  // 添加RedisTemplate冲突解决配置
    TransactionCoordinatorMockConfig.class,  // 提供TransactionCoordinator的Mock实现
    ChatCompletionsServiceMockConfig.class,  // 提供ChatCompletionsService的Mock实现
    SagaStateMachineMockConfig.class,  // 提供Saga状态机的Mock实现
    MyBatisMockConfig.class,  // 提供MyBatis相关组件的Mock实现
    GatewayAutoConfigurationDisabler.class,  // 禁用Gateway自动配置
    MockGatewayRouteConfig.class,  // 提供GatewayRouteConfig的替代实现
    TestComponentScanFilterRegistrar.class  // 注册组件扫描过滤器
})
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