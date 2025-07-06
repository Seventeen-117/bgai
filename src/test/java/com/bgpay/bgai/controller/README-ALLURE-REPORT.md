# Allure TestNG 报告使用说明

本文档说明如何使用 Allure 与 TestNG 生成交互式测试报告。

## 什么是 Allure 报告？

Allure 是一个轻量级的、灵活的、多语言支持的测试报告工具，它不仅可以以简单而优雅的方式展示测试结果，还提供了强大的测试分析功能。Allure 报告提供了以下关键特性：

- 清晰的测试执行概览
- 详细的测试步骤跟踪
- 测试失败的详细信息和错误堆栈
- 测试附件（截图、日志、HTTP请求/响应等）
- 多维度分类和筛选测试结果
- 趋势分析和统计信息

## 前提条件

- Java 17 或更高版本
- Maven 3.6 或更高版本
- Allure 命令行工具（可选，如果想在浏览器外使用）

## 项目配置

本项目已经配置好了 Allure TestNG 支持，主要配置包括：

1. **POM 依赖**:
   - `io.qameta.allure:allure-testng`
   - `io.qameta.allure:allure-java-commons`

2. **Maven 插件**:
   - `maven-surefire-plugin` 已配置 Allure 监听器
   - `allure-maven` 用于生成和展示报告

3. **TestNG 类注解**:
   - `@Epic` - 定义测试史诗（大型功能集合）
   - `@Feature` - 定义测试特性
   - `@Story` - 定义测试场景
   - `@Description` - 详细描述测试目的
   - `@Severity` - 测试严重性级别
   - `@Step` - 标记测试步骤

## 运行测试并生成报告

### Windows:
```
run-allure-report.bat
```

### Linux/Mac:
```
./run-allure-report.sh
```

或者手动执行以下命令：

```bash
# 运行测试
mvn clean test -Dtest=ChatGatWayInternalIdempotenceTest

# 生成报告
mvn allure:report

# 启动内置服务器显示报告
mvn allure:serve
```

## 报告结构

生成的 Allure 报告包含以下主要部分：

1. **Overview** - 测试执行的总体概览
2. **Categories** - 按类别（失败类型）分组的测试结果
3. **Suites** - 按测试套件分组的测试结果
4. **Graphs** - 测试执行图表统计
5. **Timeline** - 测试执行时间线

## 特殊报告元素

### 1. 步骤跟踪

所有使用 `@Step` 注解的方法都会作为测试步骤显示在报告中，如：

```java
@Step("验证控制器方法被调用 {0} 次")
private void verifyControllerMethodCalled(int times) {
    Mockito.verify(testChatGatWayInternalController, Mockito.times(times))
            .mockChatGatWayInternal(Mockito.any());
}
```

### 2. 报告附件

测试中使用 `Allure.addAttachment()` 方法添加的附件会显示在报告中：

```java
Allure.addAttachment("响应数据", "application/json", response, ".json");
```

### 3. 测试组织结构

测试使用多级标签组织：
- Epic > Feature > Story

## 自定义报告

可以通过以下方式扩展报告：

1. **添加环境变量信息**:
   在 `src/test/resources/allure.properties` 文件中定义

2. **添加自定义分类**:
   在 `src/test/resources/categories.json` 文件中定义

3. **添加历史趋势数据**:
   配置 Jenkins 或其他 CI 系统保存历史报告

## 常见问题

1. **报告不显示步骤**:
   确保正确配置了 AspectJ weaving

2. **附件未显示**:
   检查附件类型和扩展名是否匹配

3. **测试标签未分组**:
   验证 `@Epic`, `@Feature` 和 `@Story` 注解使用正确

## 进一步阅读

- [Allure 官方文档](https://docs.qameta.io/allure/)
- [Allure TestNG 集成](https://docs.qameta.io/allure/#_testng)
- [Allure Maven 插件](https://docs.qameta.io/allure/#_maven_5) 