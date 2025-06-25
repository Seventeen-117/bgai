# 测试环境问题排查与解决方案

## 主要问题

在尝试运行项目测试过程中，我们遇到了几个关键问题：

1. **缺少实体类依赖**：测试配置类`TestConfig.java`无法找到主代码中的实体类（`UserToken`, `PriceConfig`, `UsageCalculationDTO`等）

2. **数据源配置错误**：测试尝试加载完整应用上下文时，发生`数据源驱动类名为空，请检查Nacos配置是否已正确加载`错误

3. **服务发现冲突**：多个`@Primary` bean类型为`DiscoveryClient`的冲突，导致Spring无法确定使用哪一个

4. **Java版本不兼容**：测试类使用Java 17编译，但运行环境使用Java 8，导致类文件版本不兼容

## 解决方案

### 1. 为测试创建模拟实体类

我们在测试目录下创建了简化版的实体类：

```
src/test/java/com/bgpay/bgai/entity/
├── ApiConfig.java
├── PriceConfig.java
├── UsageCalculationDTO.java
├── UsageInfo.java
├── UserToken.java
└── README.md
```

这些类提供了测试所需的基本结构和默认值。

### 2. 配置测试专用配置类

我们修改了`TestConfig.java`以提供所有必要的模拟对象：

- 使用`@EnableAutoConfiguration(exclude = {...})`排除了数据库自动配置
- 增加了模拟的数据库相关Bean（DataSource, SqlSessionFactory等）
- 增加了模拟的缓存相关Bean（CacheWarmer等）
- 移除了`@Primary`注解以避免Bean冲突
- 使用`@ConditionalOnMissingBean`确保只有一个实例

### 3. 使用测试切片代替完整应用上下文

将`@SpringBootTest`替换为`@WebFluxTest`以减少加载的组件：

```java
@WebFluxTest(controllers = {ReactiveChatController.class})
@Import(TestConfig.class)
@ActiveProfiles("test")
```

### 4. Java版本兼容性

尝试解决Java版本问题的方法：

1. 修改Maven编译器配置使用Java 17
2. 创建`fix-java-for-test.bat`脚本设置正确的Java环境
3. 配置Maven Surefire插件使用正确的JVM

## 尚未解决的问题

**Java版本兼容性问题仍然存在**。即使我们尝试了多种方法来配置正确的Java环境，测试仍无法运行，因为：

- 测试类以Java 17（类文件版本61.0）编译
- 但运行时使用Java 8（只支持类文件版本52.0）

## 后续建议

1. **安装JDK 17**
   - 在开发环境中安装JDK 17
   - 设置`JAVA_HOME_17`环境变量指向JDK 17安装目录

2. **统一项目Java版本**
   - 将整个项目统一到Java 17，以避免版本不一致问题

3. **使用TestContainers**
   - 考虑使用TestContainers来模拟Redis、MySQL等外部依赖
   - 这将使测试更加可靠且接近生产环境

4. **简化测试方法**
   - 对于复杂组件，考虑编写更小的单元测试
   - 使用更多的模拟对象，减少对外部系统的依赖

## 临时解决方案

如果需要立即运行测试，建议：

1. 下载并安装JDK 17
2. 设置`JAVA_HOME`环境变量指向JDK 17
3. 使用以下命令运行测试：

```bash
set JAVA_HOME=C:\path\to\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
.\mvnw test -Dtest=SimpleUnitTest
```

## 总结

我们已经解决了实体类依赖、服务发现冲突和数据源配置问题，但Java版本兼容性问题需要在环境层面解决。项目需要使用Java 17才能正常运行测试。

## 数据库和依赖注入问题

当运行测试时，可能会遇到各种数据库相关错误：

1. **多个SqlSessionFactory beans**：
   ```
   No qualifying bean of type 'org.apache.ibatis.session.SqlSessionFactory' available: expected single matching bean but found 2: sqlSessionFactory,mybatisSqlSessionFactoryBean
   ```

2. **Nacos配置加载失败**：
   ```
   Failed to instantiate [javax.sql.DataSource]: Factory method 'masterDataSource' threw exception with message: 数据源驱动类名为空，请检查Nacos配置是否已正确加载
   ```

### 解决方案

1. **更新TestConfig**
   - 确保TestConfig提供了所有必要的模拟对象
   - 使用`@Primary`注解来处理SqlSessionFactory
   - 配置TestConfig以排除数据库自动配置
   - 提供所有必需的mapper接口的模拟实现

2. **确保测试类**
   - 导入TestConfig：`@Import(TestConfig.class)`
   - 设置测试配置文件：`@ActiveProfiles("test")`
   - 使用`@WebFluxTest`或`@ExtendWith(MockitoExtension.class)`来排除问题控制器

## 验证基本测试设置

为了验证测试环境是否设置正确，运行`SimpleUnitTest`：

```
.\mvnw test -Dtest=SimpleUnitTest
```

这是一个基本的JUnit测试，不依赖于Spring或任何外部组件。

## 推荐测试方法

鉴于应用程序的复杂性及其多个依赖项（Nacos、Redis、MySQL等）：

1. 优先考虑专注于单元测试，这些测试模拟外部依赖
2. 使用`@WebFluxTest`而不是完整的`@SpringBootTest`来减少上下文加载
3. 为不同类型的测试创建单独的测试配置
4. 确保开发和测试环境之间的Java版本一致
5. 考虑使用H2等内存数据库进行集成测试

## 其他Maven配置问题

如果项目显示关于重复依赖的构建警告：

```
'dependencies.dependency.(groupId:artifactId:type:classifier)' must be unique: 
com.alibaba:druid-spring-boot-starter:jar -> duplicate declaration of version ${druid-spring-boot-starter.version}
```

你应该清理`pom.xml`文件以删除重复的依赖项。

## Java Version Compatibility Issues

The most critical issue is the Java version mismatch between compile-time and runtime:

```
com/bgpay/bgai/controller/SimpleUnitTest has been compiled by a more recent version of the Java Runtime (class file version 61.0), this version of the Java Runtime only recognizes class file versions up to 52.0
```

This means:
- The tests are compiled with Java 17 (class file version 61.0)
- But running with Java 8 (which only supports up to version 52.0)

### Solution:

1. Install JDK 17 and set the environment variable:
   ```
   JAVA_HOME_17=C:\path\to\jdk-17
   ```

2. Run the fix-java-for-test script:
   - Windows: `fix-java-for-test.bat`
   - Linux/Mac: `./fix-java-for-test.sh`

3. Alternatively, set Java version in your IDE test runner to Java 17.

## Database and Dependency Injection Issues

When running tests, you may encounter various database-related errors:

1. **Multiple SqlSessionFactory beans**:
   ```
   No qualifying bean of type 'org.apache.ibatis.session.SqlSessionFactory' available: expected single matching bean but found 2: sqlSessionFactory,mybatisSqlSessionFactoryBean
   ```

2. **Nacos configuration loading failures**:
   ```
   Failed to instantiate [javax.sql.DataSource]: Factory method 'masterDataSource' threw exception with message: 数据源驱动类名为空，请检查Nacos配置是否已正确加载
   ```

3. **Controller Bean Creation Failures**:
   ```
   Error creating bean with name 'simpleChatController': Unsatisfied dependency expressed through constructor parameter 2: Error creating bean with name 'userMapper'
   ```

### Solutions:

1. **Exclude Controllers in WebFluxTest**:
   ```java
   @WebFluxTest(
       controllers = {ReactiveChatController.class},
       excludeFilters = @ComponentScan.Filter(
           type = FilterType.ASSIGNABLE_TYPE,
           classes = {
               EnhancedChatController.class,
               SagaStatusController.class,
               SimpleChatController.class,
               // other controllers causing problems
           }
       )
   )
   ```

2. **Mock All Required Mappers**:
   ```java
   @MockBean
   private ApiClientMapper apiClientMapper;
   
   @MockBean
   private ApiKeyMapper apiKeyMapper;
   
   @MockBean
   private UserMapper userMapper;
   
   @MockBean
   private TransactionLogMapper transactionLogMapper;
   ```

3. **Create Test-Specific Mock Beans in TestConfig**:
   ```java
   @Bean
   public UserMapper userMapper() {
       return Mockito.mock(UserMapper.class);
   }
   
   @Bean
   public TransactionLogMapper transactionLogMapper() {
       return Mockito.mock(TransactionLogMapper.class);
   }
   ```

4. **Disable Auto-configurations**:
   ```java
   @EnableAutoConfiguration(exclude = {
       DataSourceAutoConfiguration.class,
       DataSourceTransactionManagerAutoConfiguration.class,
       SeataAutoConfiguration.class
   })
   ```

5. **Set Test Properties**:
   ```java
   @TestPropertySource(properties = {
       "spring.cloud.nacos.discovery.enabled=false",
       "spring.cloud.nacos.server-addr=8.133.246.113:8848",
       "spring.autoconfigure.exclude=com.alibaba.cloud.nacos.discovery.NacosDiscoveryClientAutoConfiguration,com.alibaba.cloud.nacos.discovery.NacosDiscoveryAutoConfiguration,com.alibaba.cloud.nacos.NacosServiceAutoConfiguration,com.alibaba.cloud.nacos.NacosConfigAutoConfiguration,io.seata.spring.boot.autoconfigure.SeataAutoConfiguration",
       "seata.enabled=false"
   })
   ```

## Missing Required Beans for Test

When running the test, you may encounter dependency injection errors for various beans:

```
Error creating bean with name 'enhancedChatController': Unsatisfied dependency expressed through constructor parameter
```

### Solution:

1. Add necessary mock beans in your test class:
   ```java
   @MockBean
   private ApiClientMapper apiClientMapper;
   
   @MockBean
   private ApiKeyMapper apiKeyMapper;
   
   @MockBean
   private TransactionLogMapper transactionLogMapper;
   ```

2. Mock SAGA and Seata related components:
   ```java
   @MockBean
   private SagaStateMachineConfig sagaStateMachineConfig;
   
   @MockBean
   private CustomSagaJsonParser customSagaJsonParser;
   
   @MockBean
   private StateMachineEngine stateMachineEngine;
   ```

## Verifying Basic Test Setup

To verify that your test environment is set up correctly, run the basic test:

```java
@Test
public void testBasicEnvironmentSetup() {
    // 验证WebTestClient是否正确注入
    assertNotNull(webTestClient, "WebTestClient should be injected");
    
    // 验证必要的模拟对象是否可用
    assertNotNull(apiConfigService, "ApiConfigService mock should be available");
    assertNotNull(userMapper, "UserMapper mock should be available");
    
    System.out.println("Test environment setup successfully verified!");
}
```

## Recommended Testing Approach

Given the complexity of the application with its multiple dependencies (Nacos, Redis, MySQL, etc.):

1. Prefer focused unit tests that mock external dependencies
2. Use `@WebFluxTest` instead of full `@SpringBootTest` to reduce context loading
3. Create separate test configurations for different types of tests
4. Ensure the Java version is consistent between development and test environments
5. Consider using an in-memory database like H2 for integration tests
6. Exclude all unneeded controllers and components using `@ComponentScan(excludeFilters = ...)`
7. Mock all Mapper interfaces and database-related components
8. Disable all auto-configurations that might try to access external resources

## Additional Maven Configuration Issues

If the project shows build warnings about duplicate dependencies:

```
'dependencies.dependency.(groupId:artifactId:type:classifier)' must be unique: 
com.alibaba:druid-spring-boot-starter:jar -> duplicate declaration of version ${druid-spring-boot-starter.version}
```

You should clean up the `pom.xml` file to remove duplicate dependencies.

## RestTemplate Mock Bean Issues

You may encounter an error like this when trying to mock RestTemplate:

```
java.lang.IllegalStateException: Unable to register mock bean org.springframework.web.client.RestTemplate expected a single matching bean to replace but found [loadBalancedRestTemplate, restTemplate]
```

This happens because there are multiple RestTemplate beans in the application context, and Mockito doesn't know which one to replace.

### Solution:

1. Use the `@Qualifier` annotation to specify which bean you want to mock:

```java
@MockBean
@Qualifier("loadBalancedRestTemplate")
private RestTemplate loadBalancedRestTemplate;
```

2. Make sure your TestConfig also has named beans:

```java
@Bean(name = "loadBalancedRestTemplate")
public RestTemplate loadBalancedRestTemplate() {
    return Mockito.mock(RestTemplate.class);
}

@Bean
public RestTemplate restTemplate() {
    return Mockito.mock(RestTemplate.class);
}
```

3. Make sure you're only mocking the beans you actually need for your test.

## RocketMQ Configuration Issues

You may encounter an error related to RocketMQ configuration:

```
Error creating bean with name 'transactionMQProducer': Factory method 'transactionMQProducer' threw exception with message: the specified group[${rocketmq.producer.group}] contains illegal characters, allowing only ^[%|a-zA-Z0-9_-]+$
```

This happens because the placeholder `${rocketmq.producer.group}` is not being resolved properly in the test context.

### Solution:

1. Exclude the RocketMQConfig class from your test context:

```java
@WebFluxTest(
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {
            // Other exclusions...
            com.bgpay.bgai.service.mq.RocketMQConfig.class
        }
    )
)
```

2. Add mock RocketMQ beans in your TestConfig:

```java
@Bean
public TransactionMQProducer transactionMQProducer() {
    return Mockito.mock(TransactionMQProducer.class);
}

@Bean
public DefaultMQProducer defaultMQProducer() {
    return Mockito.mock(DefaultMQProducer.class);
}

@Bean
public DefaultMQPushConsumer defaultMQPushConsumer() {
    return Mockito.mock(DefaultMQPushConsumer.class);
}
```

3. Add explicit test properties for RocketMQ:

```java
@TestPropertySource(properties = {
    // Other properties...
    "rocketmq.producer.group=test-group",
    "rocketmq.name-server=127.0.0.1:9876"
})
```

4. Mock any controllers that depend on RocketMQ, such as UsageController:

```java
@MockBean
private UsageController usageController;
```

## Elasticsearch Repository Issues

You may encounter errors related to Elasticsearch repositories:

```
Parameter 0 of constructor in com.bgpay.bgai.filter.ChatRecordWebFilter required a bean of type 'com.bgpay.bgai.repository.es.ChatRecordRepository' that could not be found.
```

This happens because the application is trying to connect to Elasticsearch during tests.

### Solution:

1. Add the ChatRecordWebFilter to your exclusions:

```java
@WebFluxTest(
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {
            // Other exclusions...
            ChatRecordWebFilter.class
        }
    )
)
```

2. Add a mock ChatRecordRepository in your test class:

```java
@MockBean
private ChatRecordRepository chatRecordRepository;
```

3. Add a mock bean in your TestConfig:

```java
@Bean
public ChatRecordRepository chatRecordRepository() {
    return Mockito.mock(ChatRecordRepository.class);
}
```

4. Disable Elasticsearch auto-configuration in your tests:

```java
@EnableAutoConfiguration(exclude = {
    // Other exclusions...
    org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration.class
})
```

5. Add properties to disable Elasticsearch repositories:

```java
@TestPropertySource(properties = {
    // Other properties...
    "spring.data.elasticsearch.repositories.enabled=false"
})
``` 