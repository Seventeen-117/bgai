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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.mock.web.MockMultipartFile;
import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;

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
        RocketMQProducerServiceMockConfig.class,
        RocketMQTemplateMockConfig.class,
        FallbackServiceMockConfig.class,
        MockMvcConfig.class
        // 注意：不要在这里注册 ReactiveChatController 及其依赖
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
    
    /**
     * 使用YAML数据驱动测试认证API
     * 
     * @param testData YAML文件中的测试数据
     */
    @Test(dataProvider = "namedYamlData", dataProviderClass = YamlDataProvider.class)
    @YamlSource("api/auth")
    @Description("测试认证API的各种场景")
    @Story("用户认证功能")
    public void testAuthApi(Map<String, Object> testData) throws Exception {
        executeApiTest(testData, "/api/auth");
    }
    
    /**
     * 使用YAML数据驱动测试API密钥API
     * 
     * @param testData YAML文件中的测试数据
     */
    @Test(dataProvider = "namedYamlData", dataProviderClass = YamlDataProvider.class)
    @YamlSource("api/api-key")
    @Description("测试API密钥API的各种场景")
    @Story("API密钥管理功能")
    public void testApiKeyApi(Map<String, Object> testData) throws Exception {
        executeApiTest(testData, "/api/keys");
    }
    
    /**
     * 使用YAML数据驱动测试动态路由API
     * 
     * @param testData YAML文件中的测试数据
     */
    @Test(dataProvider = "namedYamlData", dataProviderClass = YamlDataProvider.class)
    @YamlSource("api/dynamic-route")
    @Description("测试动态路由API的各种场景")
    @Story("动态路由管理功能")
    public void testDynamicRouteApi(Map<String, Object> testData) throws Exception {
        executeApiTest(testData, "/gateway/routes");
    }
    
    /**
     * 使用YAML数据驱动测试系统配置API
     * 
     * @param testData YAML文件中的测试数据
     */
    @Test(dataProvider = "namedYamlData", dataProviderClass = YamlDataProvider.class)
    @YamlSource("api/system-config")
    @Description("测试系统配置API的各种场景")
    @Story("系统配置管理功能")
    public void testSystemConfigApi(Map<String, Object> testData) throws Exception {
        executeApiTest(testData, "/api/system/config");
    }
    
    /**
     * 使用YAML数据驱动测试文件上传API
     * 
     * @param testData YAML文件中的测试数据
     */
    @Test(dataProvider = "namedYamlData", dataProviderClass = YamlDataProvider.class)
    @YamlSource("api/file-upload")
    @Description("测试文件上传API的各种场景")
    @Story("文件上传管理功能")
    public void testFileUploadApi(Map<String, Object> testData) throws Exception {
        executeApiTest(testData, "/api/files");
    }
    
    /**
     * 使用YAML数据驱动测试用量API
     * 
     * @param testData YAML文件中的测试数据
     */
    @Test(dataProvider = "namedYamlData", dataProviderClass = YamlDataProvider.class)
    @YamlSource("api/usage")
    @Description("测试用量API的各种场景")
    @Story("用量管理功能")
    public void testUsageApi(Map<String, Object> testData) throws Exception {
        executeApiTest(testData, "/api/usage");
    }
    
    /**
     * 使用YAML数据驱动测试聊天API
     * 
     * @param testData YAML文件中的测试数据
     */
    @Test(dataProvider = "namedYamlData", dataProviderClass = YamlDataProvider.class)
    @YamlSource("api/testChatApi")
    @Description("测试聊天API的各种场景")
    @Story("聊天功能")
    public void testChatApi(Map<String, Object> testData) throws Exception {
        executeApiTest(testData, "/api/chat");
    }
    
    /**
     * 使用YAML数据驱动测试会话API
     * 
     * @param testData YAML文件中的测试数据
     */
    @Test(dataProvider = "namedYamlData", dataProviderClass = YamlDataProvider.class)
    @YamlSource("api/session")
    @Description("测试会话API的各种场景")
    @Story("会话管理功能")
    public void testSessionApi(Map<String, Object> testData) throws Exception {
        executeApiTest(testData, "/api/session");
    }
    
    /**
     * 使用YAML数据驱动测试用量统计API
     * 
     * @param testData YAML文件中的测试数据
     */
    @Test(dataProvider = "namedYamlData", dataProviderClass = YamlDataProvider.class)
    @YamlSource("api/usage-statistics")
    @Description("测试用量统计API的各种场景")
    @Story("用量统计功能")
    public void testUsageStatisticsApi(Map<String, Object> testData) throws Exception {
        executeApiTest(testData, "/api/usage-stats");
    }
    
    /**
     * 使用YAML数据驱动测试反应式用户API
     * 
     * @param testData YAML文件中的测试数据
     */
    @Test(dataProvider = "namedYamlData", dataProviderClass = YamlDataProvider.class)
    @YamlSource("api/reactive-users")
    @Description("测试反应式用户API的各种场景")
    @Story("反应式用户管理功能")
    public void testReactiveUsersApi(Map<String, Object> testData) throws Exception {
        executeApiTest(testData, "/api/reactive-users");
    }
    
    /**
     * 通用的API测试执行方法
     * 
     * @param testData 测试数据
     * @param baseUrl 基础URL
     */
    private void executeApiTest(Map<String, Object> testData, String baseUrl) throws Exception {
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
        Map<String, Object> parameters = (Map<String, Object>) requestData.get("parameters");
        Map<String, Object> multipart = (Map<String, Object>) requestData.get("multipart");
        
        // 获取预期响应信息
        Map<String, Object> expectedResponse = (Map<String, Object>) testData.get("expectedResponse");
        int statusCode = ((Integer) expectedResponse.get("statusCode")).intValue();
        List<String> bodyContains = (List<String>) expectedResponse.get("bodyContains");
        Map<String, Object> bodyEquals = (Map<String, Object>) expectedResponse.get("bodyEquals");
        
        // 替换路径变量
        if (pathVariables != null) {
            for (Map.Entry<String, Object> entry : pathVariables.entrySet()) {
                endpoint = endpoint.replace("{" + entry.getKey() + "}", entry.getValue().toString());
            }
        }

        // 修正url拼接逻辑，防止重复斜杠
        String url;
        if (endpoint == null || endpoint.isEmpty()) {
            url = baseUrl;
        } else if (endpoint.startsWith("/")) {
            url = baseUrl + endpoint;
        } else {
            url = baseUrl + "/" + endpoint;
        }

        MockHttpServletRequestBuilder requestBuilder;
        
        switch (method) {
            case "GET":
                requestBuilder = MockMvcRequestBuilders.get(url);
                break;
            case "POST":
                requestBuilder = MockMvcRequestBuilders.post(url);
                break;
            case "PUT":
                requestBuilder = MockMvcRequestBuilders.put(url);
                break;
            case "DELETE":
                requestBuilder = MockMvcRequestBuilders.delete(url);
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
        
        // 添加查询参数
        if (parameters != null) {
            for (Map.Entry<String, Object> param : parameters.entrySet()) {
                requestBuilder = requestBuilder.param(param.getKey(), param.getValue().toString());
            }
        }
        
        // 添加请求体或multipart数据
        if (body != null) {
            requestBuilder = requestBuilder
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body));
        } else if (multipart != null) {
            requestBuilder = MockMvcRequestBuilders.multipart(baseUrl + endpoint);

            // 添加文件
            if (multipart.containsKey("file")) {
                String fileName = (String) multipart.get("file");
                String contentType = (String) multipart.get("contentType");
                String fileContent = (String) multipart.get("fileContent");

                if (fileContent != null) {
                    requestBuilder = ((MockMultipartHttpServletRequestBuilder) requestBuilder).file(
                        new MockMultipartFile(
                            "file",
                            fileName,
                            contentType != null ? contentType : "text/plain",
                            fileContent.getBytes()
                        )
                    );
                }
            }

            // 添加其他参数
            for (Map.Entry<String, Object> entry : multipart.entrySet()) {
                if (!entry.getKey().equals("file") && !entry.getKey().equals("contentType") && !entry.getKey().equals("fileContent")) {
                    requestBuilder = ((MockMultipartHttpServletRequestBuilder) requestBuilder).param(entry.getKey(), entry.getValue().toString());
                }
            }
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

