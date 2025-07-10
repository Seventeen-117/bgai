package com.bgpay.bgai.controller;

import com.bgpay.bgai.controller.AuthController;
import com.bgpay.bgai.controller.FeignDemoController;
import com.bgpay.bgai.controller.ServiceDiscoveryExampleController;

import com.bgpay.bgai.base.YamlDataProvider;
import com.bgpay.bgai.base.YamlSource;
import com.bgpay.bgai.config.BillingServiceMockConfig;
import com.bgpay.bgai.config.ChatRecordRepositoryMockConfig;
import com.bgpay.bgai.config.ChatWebFilterMockConfig;
import com.bgpay.bgai.config.DeepSeekServiceMockConfig;
import com.bgpay.bgai.config.DeepSeekWebClientMockConfig;
import com.bgpay.bgai.config.CacheWarmerMockConfig;
import com.bgpay.bgai.config.ElasticsearchAutoConfigurationDisabler;
import com.bgpay.bgai.config.ElasticsearchMockConfig;
import com.bgpay.bgai.config.EnhancedChatControllerMockConfig;
import com.bgpay.bgai.config.DataSourceAutoConfigurationDisabler;
import com.bgpay.bgai.config.FileTypeServiceMockConfig;
import com.bgpay.bgai.config.FeignAutoConfigurationDisabler;
import com.bgpay.bgai.config.RedisAutoConfigurationDisabler;
import com.bgpay.bgai.config.ExcludeMainWebClientConfig;
import com.bgpay.bgai.config.MockMvcTestConfig;
import com.bgpay.bgai.config.ReactiveWebMockConfig;
import com.bgpay.bgai.config.RocketMQMockConfig;
import com.bgpay.bgai.config.TestPropertySourceConfig;
import com.bgpay.bgai.config.UnifiedEnvironmentConfig;
import com.bgpay.bgai.config.WebClientAutoConfigurationExcluder;
import com.bgpay.bgai.config.WebClientBeanExclusionConfig;
import com.bgpay.bgai.config.WebClientBuilderUnifier;
import com.bgpay.bgai.config.WebClientConfigMock;
import com.bgpay.bgai.config.WebClientConfigOverrideConfig;
import com.bgpay.bgai.config.WebClientTestConfig;
import com.bgpay.bgai.config.WebClientTestOverrideConfig;
import com.bgpay.bgai.config.WebMvcAutoConfigurationDisabler;
import com.bgpay.bgai.config.RedisTemplateQualifierConfig;
import com.bgpay.bgai.config.WebClientConfigExcluder;
import com.bgpay.bgai.config.GatewayRouteMockConfig;
import com.bgpay.bgai.config.GatewayRouteConfigMock;
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
import com.bgpay.bgai.config.WebClientCircularDependencyResolver;
import com.bgpay.bgai.config.WebClientQualifierResolver;

/**
 * 基于YAML数据驱动的API测试示例
 */
@Feature("API测试")
@WebMvcTest(
    controllers = {AuthController.class, ServiceDiscoveryExampleController.class, FeignDemoController.class},
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
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
@AutoConfigureMockMvc
@ActiveProfiles("test")  // 确保使用test profile，激活WebClientTestReplacer
@Import({
    WebClientCircularDependencyResolver.class,  // 解决WebClient循环依赖问题，最高优先级
    WebClientConfigExcluder.class,  // 排除主应用中的WebClientConfig
    WebClientQualifierResolver.class,  // 解决WebClient多主键冲突问题
    GatewayRouteMockConfig.class,  // 提供Gateway路由组件的Mock实现
    GatewayRouteConfigMock.class,  // 禁用主应用中的Gateway路由配置
    UnifiedEnvironmentConfig.class,  // 提供统一的Environment配置，解决环境bean冲突
    MockMvcTestConfig.class, 
    WebMvcAutoConfigurationDisabler.class,
    WebClientTestConfig.class,
    WebClientBuilderUnifier.class,  // 提供统一的WebClient.Builder实现，避免冲突
    WebClientTestOverrideConfig.class,  // 使用高优先级配置覆盖WebClientConfig
    WebClientAutoConfigurationExcluder.class,
    ExcludeMainWebClientConfig.class,  // 明确排除主应用中的WebClientConfig
    WebClientBeanExclusionConfig.class,  // 明确替换WebClientConfig bean
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
    FeignAutoConfigurationDisabler.class,  // 禁用OpenFeign自动配置，使用Mock实现
    RedisTemplateQualifierConfig.class  // 添加RedisTemplate冲突解决配置
})
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "seata.enabled=false",
    "saga.enabled=false",
    "seata.saga.state-machine.auto-register=false",
    "spring.cloud.loadbalancer.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "spring.cloud.config.enabled=false",
    "spring.cloud.consul.enabled=false",
    "spring.cloud.gateway.enabled=false",
    "spring.cloud.circuitbreaker.enabled=false",
    "spring.main.web-application-type=servlet",
    "spring.validation.is-method-validation-bean-post-processor-enabled=false",
    "rocketmq.messageConsumer.enabled=false",
    "rocketmq.producer.enable=false",
    "rocketmq.name-server=8.133.246.113:9876",
    "rocketmq.producer.group=test-group",
    "spring.data.elasticsearch.repositories.enabled=false",
    "spring.elasticsearch.enabled=false",
    "spring.webflux.base-path=/api",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration"
})
public class YamlDataDrivenApiTest extends AbstractTestNGSpringContextTests {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected String testId;
    
    @Autowired
    private MockMvc mockMvc;
    
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
        
        // 获取预期响应信息
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
                requestBuilder.header(header.getKey(), header.getValue().toString());
            }
        }
        
        // 添加请求体
        if (body != null && (method.equals("POST") || method.equals("PUT"))) {
            requestBuilder.contentType(MediaType.APPLICATION_JSON);
            requestBuilder.content(objectMapper.writeValueAsString(body));
        }
        
        // 执行请求
        ResultActions resultActions = mockMvc.perform(requestBuilder);
        
        // 验证状态码
        resultActions.andExpect(MockMvcResultMatchers.status().is(statusCode));
        
        // 验证响应体包含的内容
        if (bodyContains != null) {
            for (String containText : bodyContains) {
                resultActions.andExpect(MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString(containText)));
            }
        }
        
        // 验证响应体的精确匹配
        if (bodyEquals != null) {
            for (Map.Entry<String, Object> entry : bodyEquals.entrySet()) {
                resultActions.andExpect(MockMvcResultMatchers.jsonPath("$." + entry.getKey()).value(entry.getValue()));
            }
        }
    }
    
    /**
     * 使用指定YAML文件测试用户API
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
                String value = header.getValue().toString();
                // 替换令牌变量
                if (value.contains("${token}")) {
                    value = value.replace("${token}", "sample-jwt-token-for-testing");
                }
                requestBuilder.header(header.getKey(), value);
            }
        }
        
        // 添加请求体
        if (body != null && (method.equals("POST") || method.equals("PUT"))) {
            requestBuilder.contentType(MediaType.APPLICATION_JSON);
            requestBuilder.content(objectMapper.writeValueAsString(body));
        }
        
        // 执行请求
        ResultActions resultActions = mockMvc.perform(requestBuilder);
        
        // 验证状态码
        resultActions.andExpect(MockMvcResultMatchers.status().is(statusCode));
        
        // 验证响应体包含的内容
        if (bodyContains != null) {
            for (String containText : bodyContains) {
                resultActions.andExpect(MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString(containText)));
            }
        }
        
        // 验证响应体的精确匹配
        if (bodyEquals != null) {
            for (Map.Entry<String, Object> entry : bodyEquals.entrySet()) {
                resultActions.andExpect(MockMvcResultMatchers.jsonPath("$." + entry.getKey()).value(entry.getValue()));
            }
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