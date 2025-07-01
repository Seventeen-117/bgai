# 动态路由测试指南

本文档介绍如何测试Spring Cloud Gateway的动态路由功能。

## 前提条件

1. 确保应用已经启动，并且Gateway功能已启用
2. 确认`application-dev.yml`中的配置：`bgai.gateway.enabled=true`
3. 确保Nacos服务已启动并可访问

## 测试方法

### 方法一：使用批处理脚本

在Windows环境下，可以直接运行`test-dynamic-route.bat`：

```bash
test-dynamic-route.bat
```

在Linux/Mac环境下，可以运行`test-dynamic-route.sh`：

```bash
chmod +x test-dynamic-route.sh
./test-dynamic-route.sh
```

### 方法二：使用Postman

1. 导入`test-dynamic-route-postman.json`到Postman
2. 按照集合中的请求顺序依次测试各个API

### 方法三：手动测试

可以使用curl命令手动测试各个API：

#### 1. 获取所有路由

```bash
curl -X GET http://localhost:8688/gateway/routes
```

#### 2. 添加单个路由

```bash
curl -X POST http://localhost:8688/gateway/routes \
  -H "Content-Type: application/json" \
  -d @test-dynamic-route.json
```

#### 3. 获取特定路由

```bash
curl -X GET http://localhost:8688/gateway/routes/api-test-route
```

#### 4. 批量添加路由

```bash
curl -X POST http://localhost:8688/gateway/routes/batch \
  -H "Content-Type: application/json" \
  -d @test-dynamic-routes-batch.json
```

#### 5. 更新路由

```bash
curl -X PUT http://localhost:8688/gateway/routes \
  -H "Content-Type: application/json" \
  -d '{
    "id": "api-test-route",
    "predicates": [
      {
        "name": "Path",
        "args": {
          "pattern": "/api/test-updated/**"
        }
      }
    ],
    "filters": [
      {
        "name": "StripPrefix",
        "args": {
          "parts": "1"
        }
      },
      {
        "name": "AddResponseHeader",
        "args": {
          "name": "X-Response-From",
          "value": "API-Gateway-Updated"
        }
      }
    ],
    "uri": "http://localhost:8688/test-updated",
    "order": 0
  }'
```

#### 6. 刷新路由

```bash
curl -X POST http://localhost:8688/gateway/routes/refresh
```

#### 7. 删除路由

```bash
curl -X DELETE http://localhost:8688/gateway/routes/api-test-route
```

#### 8. 批量删除路由

```bash
curl -X DELETE http://localhost:8688/gateway/routes/batch \
  -H "Content-Type: application/json" \
  -d '["api-user-route", "api-product-route"]'
```

## 路由定义格式

路由定义的JSON格式如下：

```json
{
  "id": "路由ID",
  "predicates": [
    {
      "name": "谓词名称",
      "args": {
        "参数名": "参数值"
      }
    }
  ],
  "filters": [
    {
      "name": "过滤器名称",
      "args": {
        "参数名": "参数值"
      }
    }
  ],
  "uri": "目标URI",
  "order": 0
}
```

### 常用谓词（Predicates）

- `Path`：路径匹配
- `Method`：HTTP方法匹配
- `Host`：主机名匹配
- `Header`：请求头匹配
- `Query`：查询参数匹配
- `Cookie`：Cookie匹配
- `After`/`Before`/`Between`：时间匹配

### 常用过滤器（Filters）

- `StripPrefix`：去除前缀
- `AddRequestHeader`：添加请求头
- `AddResponseHeader`：添加响应头
- `RewritePath`：重写路径
- `PrefixPath`：添加前缀
- `RequestRateLimiter`：请求限流

## 注意事项

1. 路由ID必须唯一，如果添加重复ID的路由，会覆盖原有路由
2. 路由定义会持久化到Nacos配置中心，应用重启后仍然有效
3. 如果路由配置有误，可能会导致路由不生效
4. 使用`/gateway/routes/refresh`可以强制刷新路由配置

## 故障排除

1. 如果路由不生效，检查路由定义是否正确
2. 检查Nacos连接是否正常
3. 查看应用日志中是否有路由相关的错误信息
4. 确认Gateway功能是否正确启用 