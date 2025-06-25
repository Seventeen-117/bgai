# X-User-Id 头部测试指南

本文档介绍如何测试 `/api/chatGatWay-internal` 接口对 `X-User-Id` 头部的处理。

## 背景

在之前的实现中，`/api/chatGatWay-internal` 接口会使用多种方式获取用户ID，包括：

1. 通过认证令牌解析
2. 从请求属性中获取
3. 当以上方法都失败时，使用 "default" 作为默认值

现在我们增强了该接口，可以通过 `X-User-Id` 头部直接传递用户ID，优先级高于其他方式。

## 安全更新说明

**重要安全更新**：为了提高系统安全性，我们对 `/api/chatGatWay-internal` 接口的 `X-User-Id` 处理逻辑进行了以下改进：

1. 使用 `X-User-Id` 头部时，**必须同时提供有效的 Authorization 令牌**
2. Authorization 令牌必须是有效的（未过期）
3. 令牌对应的用户ID必须与 `X-User-Id` 值完全匹配
4. 如果不满足上述任何条件，请求将被拒绝（返回 401 或 403 错误）

这确保了即使旧的访问令牌已被刷新或过期，它也不能再与 `X-User-Id` 一起使用访问系统。

## 测试方法

### 1. 使用提供的 HTML 测试工具

我们提供了一个简单的 HTML 测试页面 `test-x-user-id.html`，可以直接在浏览器中使用：

1. 启动服务器
2. 在浏览器中打开 `http://localhost:8688/test-x-user-id.html`
3. 填写表单
   - 用户ID: 设置你想要使用的用户ID（默认 "689258T"）
   - 访问令牌: 输入与用户ID匹配的有效访问令牌（必填项）
   - 问题内容: 输入你的问题
   - 其他可选参数
4. 点击"发送请求"按钮

系统将通过 `X-User-Id` 头部和 `Authorization` 头部发送请求，并检查返回的 `usage.userId` 是否与发送的ID匹配。

### 2. 使用 cURL 命令行测试

```bash
curl -X POST http://localhost:8688/api/chatGatWay-internal \
     -H "X-User-Id: 689258T" \
     -H "Authorization: Bearer YOUR_VALID_ACCESS_TOKEN" \
     -F "question=请分析Java中的乐观锁实现方法" \
     -F "modelName=deepseek-reasoner" \
     -F "multiTurn=false"
```

## 3. 使用 Postman 测试

1. 创建一个 POST 请求到 `http://localhost:8688/api/chatGatWay-internal`
2. 在"Headers"部分添加：
   - `X-User-Id: 689258T`
   - `Authorization: Bearer YOUR_VALID_ACCESS_TOKEN`
3. 在"Body"部分选择 `form-data`，添加以下键值对：
   - `question`: 请分析Java中的乐观锁实现方法
   - `modelName`: deepseek-reasoner
   - `multiTurn`: false
4. 发送请求并检查响应

## 预期结果

无论使用哪种测试方法，响应中的 `usage.userId` 字段都应该与请求头中的 `X-User-Id` 值一致。

如果令牌无效或令牌用户ID与 `X-User-Id` 不匹配，将收到 401 或 403 错误响应。

## 测试用例

1. **有效令牌匹配测试**：使用有效的用户ID和匹配的有效令牌，验证响应中包含相同的ID
2. **令牌过期测试**：使用过期的令牌，验证是否返回401错误
3. **令牌不匹配测试**：使用有效令牌但与X-User-Id不匹配，验证是否返回403错误
4. **缺少令牌测试**：仅提供X-User-Id但不提供令牌，验证是否返回401错误

## 获取有效令牌

要获取有效的访问令牌用于测试，可以使用以下方法之一：

1. 通过刷新用户令牌：
   ```bash
   curl -X POST http://localhost:8688/api/auth/refresh-by-userid?userId=689258T \
        -H "Authorization: Bearer ADMIN_TOKEN"
   ```

2. 使用简单认证接口（仅限开发环境）：
   ```bash
   curl -X POST http://localhost:8688/api/simple-auth/login?userId=689258T
   ```

## 单元测试

我们还增加了自动化测试用例，可以通过运行以下命令执行：

```bash
mvn test -Dtest=SimpleChatGatewayInternalTest
```

此测试类专门验证`X-User-Id`头部的处理逻辑和安全性。 