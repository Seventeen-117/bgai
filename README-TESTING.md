# BGAI 测试框架使用指南

## 概述

BGAI 测试框架基于 TestNG 和 Allure 构建，提供了全面的测试功能，包括单元测试、集成测试、API 测试等。该框架支持传统的 MVC 控制器测试和响应式 WebFlux 控制器测试，并生成详细的 Allure 测试报告。



graph TD
A[测试数据YAML文件] --> B[YamlDataProvider]
B --> C[测试方法]
D[YamlSource注解] --> B
E[YamlTestUtils] --> B
E --> C
F[测试结果] --> G[Allure报告]
C --> F

    subgraph 数据驱动测试流程
        A
        B
        C
        D
        E
    end
    
    subgraph 报告生成
        F
        G
    end

## 技术栈

- **TestNG**: 测试执行框架
- **Spring Boot Test**: Spring Boot 应用测试支持
- **Allure**: 测试报告生成工具
- **MockMvc**: MVC 控制器测试工具
- **WebTestClient**: WebFlux 控制器测试工具

## 目录结构

```
src/test/
├── java/com/bgpay/bgai/
│   ├── base/                       # 测试基类
│   │   ├── BaseTestNGSpringContextTests.java   # 基础测试类
│   │   ├── AbstractWebMvcTest.java            # MVC 测试基类
│   │   └── AbstractWebFluxTest.java           # WebFlux 测试基类
│   ├── controller/                 # 控制器测试
│   │   ├── SampleControllerTest.java          # MVC 控制器测试示例
│   │   └── SampleWebFluxControllerTest.java   # WebFlux 控制器测试示例
│   ├── service/                    # 服务层测试
│   └── ...
└── resources/
    ├── testng.xml                  # TestNG 配置文件
    ├── allure.properties           # Allure 配置文件
    └── ...
```

## 基础测试类

### BaseTestNGSpringContextTests

所有测试类的基类，提供通用的测试功能：

```java
@SpringBootTest(
    classes = BgaiApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public abstract class BaseTestNGSpringContextTests extends AbstractTestNGSpringContextTests {
    // ...
}
```

主要功能：
- 加载 Spring 应用上下文
- 提供日志记录
- 支持 Allure 报告生成
- 提供测试生命周期方法

### AbstractWebMvcTest

MVC 控制器测试的基类：

```java
@AutoConfigureMockMvc
public abstract class AbstractWebMvcTest extends BaseTestNGSpringContextTests {
    @Autowired
    protected MockMvc mockMvc;
    
    // 提供 HTTP 请求方法：GET, POST, PUT, DELETE 等
}
```

### AbstractWebFluxTest

WebFlux 控制器测试的基类：

```java
@AutoConfigureWebTestClient
public abstract class AbstractWebFluxTest extends BaseTestNGSpringContextTests {
    @Autowired
    protected WebTestClient webTestClient;
    
    // 提供响应式 HTTP 请求方法
}
```

## 测试分组

在 `testng.xml` 中定义了以下测试分组：

- **controller-tests**: 控制器测试
- **service-tests**: 服务层测试
- **integration-tests**: 集成测试
- **unit-tests**: 单元测试

可以通过以下方式指定测试分组：

```java
@Test(groups = {"controller-tests", "user-api"})
public void testMethod() {
    // ...
}
```

## 运行测试

### 使用 Maven 运行所有测试

```bash
mvn clean test
```

### 运行特定测试类

```bash
mvn clean test -Dtest=SampleControllerTest
```

### 运行特定测试方法

```bash
mvn clean test -Dtest=SampleControllerTest#testGetUsers
```

### 运行特定测试组

```bash
mvn clean test -Dgroups=controller-tests
```

## 生成 Allure 报告

### 生成报告

```bash
mvn allure:report
```

### 查看报告

```bash
mvn allure:serve
```

或者使用提供的脚本：

```bash
# Windows
.\run-allure-report.bat

# Linux/Mac
./run-allure-report.sh
```

## 编写测试

### MVC 控制器测试示例

```java
@Epic("API测试")
@Feature("示例控制器")
@Owner("测试团队")
public class SampleControllerTest extends AbstractWebMvcTest {

    @Test(groups = {"controller-tests", "user-api"})
    @Story("用户管理")
    @Description("测试获取用户列表接口，验证返回状态码和响应格式")
    @Severity(SeverityLevel.CRITICAL)
    public void testGetUsers() throws Exception {
        logStep("发送GET请求到/api/users");
        
        performGet("/api/users")
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
            .andExpect(jsonPath("$.success", is(true)));
        
        logStep("验证响应成功");
    }
}
```

### WebFlux 控制器测试示例

```java
@Epic("API测试")
@Feature("响应式控制器")
@Owner("测试团队")
public class SampleWebFluxControllerTest extends AbstractWebFluxTest {

    @Test(groups = {"controller-tests", "reactive-api"})
    @Story("消息管理")
    @Description("测试获取消息列表接口，验证返回状态码和响应格式")
    @Severity(SeverityLevel.CRITICAL)
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
}
```

## Allure 注解

Allure 提供了丰富的注解来增强测试报告：

- `@Epic`: 定义测试的史诗级别分类
- `@Feature`: 定义测试的功能分类
- `@Story`: 定义测试的故事分类
- `@Description`: 提供测试的详细描述
- `@Severity`: 定义测试的严重性级别
- `@Owner`: 指定测试的负责人
- `@Issue`: 关联 Issue 跟踪系统中的问题
- `@TmsLink`: 关联测试管理系统中的测试用例

## 最佳实践

1. **测试分层**：遵循测试金字塔原则，编写更多的单元测试，适量的集成测试和少量的端到端测试。

2. **测试隔离**：每个测试应该是独立的，不依赖于其他测试的执行结果。

3. **测试数据管理**：使用 `@BeforeMethod` 和 `@AfterMethod` 来设置和清理测试数据。

4. **断言消息**：提供有意义的断言消息，以便在测试失败时能够更容易地诊断问题。

5. **测试命名**：使用描述性的测试方法名称，遵循 `testXxx_WhenYyy_ThenZzz` 的命名约定。

6. **测试分组**：合理使用测试分组，以便有选择地运行测试。

7. **日志和附件**：使用 `logStep` 和 `addAttachment` 方法记录测试步骤和相关信息。

## 常见问题

### Q: 如何模拟依赖服务？

A: 使用 `@MockBean` 注解模拟依赖服务，例如：

```java
@MockBean
private UserService userService;

@BeforeMethod
public void setUp() {
    when(userService.getUser(anyString())).thenReturn(new User("123", "测试用户"));
}
```

### Q: 如何处理异步测试？

A: 对于 WebFlux 测试，可以使用 `StepVerifier` 来验证异步结果：

```java
StepVerifier.create(userService.findById("123"))
    .expectNextMatches(user -> user.getId().equals("123"))
    .verifyComplete();
```

### Q: 如何测试异常情况？

A: 使用 TestNG 的 `expectedExceptions` 属性或者断言异常：

```java
@Test(expectedExceptions = UserNotFoundException.class)
public void testGetUser_WhenUserNotFound_ThenThrowException() {
    userService.getUser("non-existent-id");
}
```

## 参考资料

- [TestNG 官方文档](https://testng.org/doc/)
- [Allure 官方文档](https://docs.qameta.io/allure/)
- [Spring Boot 测试指南](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [WebTestClient 文档](https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html#webtestclient)

## YAML数据驱动测试

BGAI 项目支持使用 YAML 文件作为数据源进行数据驱动测试，这使得测试用例更加清晰、易于维护。

### YAML 数据结构

测试数据 YAML 文件通常包含以下结构：

```yaml
# 测试描述
description: "API测试用例"
baseUrl: "/api/users"
# 测试用例列表
testCases:
  - id: "test-case-1"
    description: "测试场景描述"
    request:
      method: "GET"
      headers:
        Authorization: "Bearer ${token}"
      pathVariables:
        userId: "1001"
    expectedResponse:
      statusCode: 200
      bodyContains:
        - "expectedValue1"
        - "expectedValue2"
```

### 使用 YamlDataProvider

项目提供了 `YamlDataProvider` 类，用于加载 YAML 测试数据：

```java
@Test(dataProvider = "yamlData", dataProviderClass = YamlDataProvider.class)
public void testApiEndpoint(Map<String, Object> testData) {
    // 使用 testData 中的数据进行测试
}
```

默认情况下，`YamlDataProvider` 会根据测试类名和方法名查找对应的 YAML 文件：
`src/test/resources/test-data/{TestClassName}/{testMethodName}.yml`

### 使用 YamlSource 注解

也可以使用 `@YamlSource` 注解指定 YAML 文件：

```java
@Test(dataProvider = "namedYamlData", dataProviderClass = YamlDataProvider.class)
@YamlSource("api/users")
public void testUserApi(Map<String, Object> testData) {
    // 使用指定 YAML 文件中的数据进行测试
}
```

### 变量替换

YAML 文件中可以使用 `${variable}` 语法定义变量占位符，在测试运行时会被替换：

```yaml
headers:
  Authorization: "Bearer ${token}"
```

可以使用 `YamlTestUtils` 类进行变量替换：

```java
Map<String, String> variables = new HashMap<>();
variables.put("token", "actual-token-value");
YamlTestUtils.replaceVariablesInMap(testData, variables);
``` 