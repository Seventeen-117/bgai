# BGAI 智能服务平台

## 项目简介

BGAI 是一套基于 Spring Boot 3、Spring Cloud Alibaba、Seata、MyBatis-Plus、RocketMQ、Elasticsearch、Redis/Redisson、Pinia+Vue3 的企业级智能 AI 服务平台。支持多数据源、分布式事务、微服务注册与配置、消息队列、缓存、全文检索等能力，前后端分离，支持高并发与高可用。

---

## 技术架构图

```mermaid
flowchart TD
  subgraph 前端
    FE["Vue3 + Vite + Pinia<br/>axios"]
  end
  subgraph 网关/接口层
    GW["Spring WebFlux Controller<br/>（RESTful API）"]
  end
  subgraph 服务层
    SVC["Service 层<br/>（业务逻辑、幂等、鉴权、Saga、分布式事务）"]
  end
  subgraph 数据层
    DB["MySQL (多数据源, Seata 代理)"]
    REDIS["Redis/Redisson<br/>（缓存/分布式锁）"]
    ES["Elasticsearch"]
  end
  subgraph 中间件
    MQ["RocketMQ"]
    NACOS["Nacos<br/>（注册/配置中心）"]
    SEATA["Seata Server"]
  end
  subgraph 日志追踪
    LOG["LogTracing<br/>(MDC-based)"]
  end
  subgraph 服务发现
    SD["服务发现和动态路由<br/>(Nacos + LoadBalancer)"]
  end
  FE-->|HTTP|GW
  GW-->|调用|SVC
  SVC-->|ORM|DB
  SVC-->|缓存/锁|REDIS
  SVC-->|消息|MQ
  SVC-->|全文检索|ES
  SVC-->|注册/配置|NACOS
  SVC-->|分布式事务|SEATA
  MQ-->|异步事件|SVC
  NACOS-->|配置推送|SVC
  SEATA-->|事务协调|SVC
  DB-->|数据|SVC
  REDIS-->|缓存|SVC
  ES-->|检索|SVC
  GW-->|跟踪|LOG
  SVC-->|跟踪|LOG
  SVC-->|服务发现|SD
  SD-->|服务查询|NACOS
```

---

## 技术栈

### 后端

- **Spring Boot 3.2.x**
- **Spring Cloud 2023.x** + **Spring Cloud Alibaba**
- **Seata**（分布式事务）
- **MyBatis-Plus**（高效 ORM）
- **Druid**（多数据源、SQL监控）
- **RocketMQ**（消息队列）
- **Elasticsearch**（全文检索）
- **Redis/Redisson**（缓存/分布式锁）
- **Caffeine**（本地缓存）
- **Nacos**（注册中心/配置中心）
- **Log Tracing**（基于 MDC 的分布式日志追踪）
- **动态路由**（基于 Nacos 的服务发现和负载均衡）

### 前端

- **Vue 3**
- **Vite**
- **Pinia**（状态管理）
- **Vue Router**
- **Axios**
- **ESLint**

---

## 目录结构

```
bgai/
├── src/
│   ├── main/
│   │   ├── java/com/bgpay/bgai/    # 后端主代码
│   │   └── resources/              # 配置、SQL、Saga、Mapper等
│   └── test/                       # 后端测试
├── frontend/                       # 前端 Vue3 + Vite + Pinia
├── Dockerfile                      # 标准 Docker 构建文件
├── Dockerfile.quick               # 快速构建 Docker 文件（针对中国网络环境优化）
├── docker-compose.yml              # 一键启动依赖服务
├── README.md                       # 项目说明
├── README-dockerfile.md           # Docker 构建详细说明
└── ...                             # 其他脚本、文档、工具
```

---

## 快速启动

### 1. 环境准备

- JDK 21
- Maven 3.8+
- Node.js 18+
- Docker & Docker Compose

### 2. 启动依赖服务

```bash
docker-compose up -d
```
> 包含 MySQL、Redis、Nacos、RocketMQ、Elasticsearch、Seata-Server 等

### 3. 启动后端

```bash
./mvnw clean package -DskipTests
java -jar target/bgai-0.0.1-SNAPSHOT.jar
# 或
./start.sh
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```
访问：http://localhost:5173

### 5. Docker 构建和运行

```bash
# 标准构建
docker build -t bgai:latest .

# 快速构建（优化中国网络环境）
docker build -f Dockerfile.quick -t bgai:latest .

# 运行容器
docker run -p 8688:8688 -e SPRING_PROFILES_ACTIVE=dev bgai:latest
```
> 更多 Docker 构建选项详见 [Docker 构建指南](README-dockerfile.md)

---

## 主要功能

- 多数据源动态路由
- 分布式事务（Seata 自动代理）
- 微服务注册/配置（Nacos）
- 消息队列（RocketMQ）
- 缓存/分布式锁（Redis/Redisson/Caffeine）
- 全文检索（Elasticsearch）
- Saga 状态机
- 前后端分离
- 分布式日志追踪（基于 MDC 的 traceId 和 userId 追踪）
- 动态服务发现和路由（基于 Nacos 的服务发现和负载均衡）
- 丰富的脚本和 Docker 支持

---

## 重要约定与最佳实践

- 数据源代理：所有物理数据源由 Seata 自动代理，dynamicDataSource 只做路由。
- 分布式事务：只需在业务方法上加 `@GlobalTransactional`。
- 配置管理：所有环境变量、数据库连接、MQ等均可通过 Nacos 配置中心集中管理。
- 前端开发：推荐使用 VSCode + Volar 插件。
- 日志追踪：所有请求会自动分配 traceId，可通过日志追踪完整调用链路。
- 服务调用：使用动态路由工具类进行微服务间通信，支持同步和响应式调用方式。

---

## 日志追踪系统

系统集成了基于 MDC 的分布式日志追踪功能：

- 每个请求自动分配唯一 traceId
- 自动关联用户 userId（如果已认证）
- 支持 WebMvc 和 WebFlux 两种 Web 框架
- 日志格式包含 traceId 和 userId，便于问题排查
- 与 Logstash 集成，支持集中式日志分析

使用示例：
```java
// 不需要手动设置 traceId，拦截器会自动处理
// 但如有需要，可以手动获取当前 trace 信息
String traceId = LogUtils.getTraceId();
String userId = LogUtils.getUserId();

// 记录业务日志时会自动包含追踪信息
logger.info("业务操作完成");
```

---

## 服务发现和动态路由

系统集成了基于 Nacos 的服务发现和动态路由功能：

- 支持服务自动注册到 Nacos 注册中心
- 支持基于服务名的负载均衡调用（无需硬编码 IP/端口）
- 同时支持响应式（WebClient）和传统（RestTemplate）两种调用方式
- 提供统一的 ServiceDiscoveryUtils 工具类简化服务调用

### 使用示例：

#### 响应式调用（WebClient）

```java
// 注入工具类
@Autowired
private ServiceDiscoveryUtils serviceDiscoveryUtils;

// 调用远程服务（使用服务名）
Mono<UserDto> userMono = serviceDiscoveryUtils.callService(
    "user-service",          // 服务名
    "/api/users/123",        // 接口路径
    HttpMethod.GET,          // HTTP方法
    null,                    // 请求体（GET请求为null）
    UserDto.class            // 响应类型
);
```

#### 同步调用（RestTemplate）

```java
// 注入工具类
@Autowired
private ServiceDiscoveryUtils serviceDiscoveryUtils;

// 同步调用远程服务
UserDto user = serviceDiscoveryUtils.callServiceSync(
    "user-service",          // 服务名
    "/api/users/123",        // 接口路径
    HttpMethod.GET,          // HTTP方法
    null,                    // 请求体（GET请求为null）
    UserDto.class            // 响应类型
);
```

#### 获取服务信息

```java
// 获取指定服务的所有实例
List<ServiceInstance> instances = serviceDiscoveryUtils.getServiceInstances("user-service");

// 获取指定服务的一个实例（负载均衡）
Optional<ServiceInstance> instance = serviceDiscoveryUtils.getServiceInstance("user-service");

// 构建完整的服务URL
String serviceUrl = serviceDiscoveryUtils.buildServiceUrl("user-service", "/api/endpoint");
```

---

## Docker 部署指南

### 系统要求

- Docker 20.10.0 或更高版本
- Docker Compose 2.0.0 或更高版本
- 至少 4GB 内存
- 至少 10GB 磁盘空间

### 技术说明

- 本 Docker 镜像基于 OpenJDK 17 (Eclipse Temurin)
- 应用程序在构建时自动配置为 Java 17 兼容模式
- 适用于 Spring Boot 3.0.x 项目

### 解决 Java 版本问题

如果您在构建时遇到 `invalid target release: 21` 错误，表明您的项目配置为使用 Java 21，但服务器上只有 Java 17。解决此问题有以下几种方式：

#### 方法1：使用独立的 Dockerfile

使用项目根目录下的 `Dockerfile.standalone` 文件进行构建：

```bash
docker build -f Dockerfile.standalone -t bgai:latest .
```

#### 方法2：使用修复脚本

1. 使脚本可执行：
   ```bash
   chmod +x fix-java-version.sh
   ```

2. 运行脚本修复 pom.xml 中的 Java 版本：
   ```bash
   ./fix-java-version.sh
   ```

3. 构建 Docker 镜像：
   ```bash
   docker build -t bgai:latest .
   ```

#### 方法3：手动修改 pom.xml

1. 编辑 pom.xml 文件：
   ```bash
   cp pom.xml pom.xml.original
   sed -i 's/<java.version>21<\/java.version>/<java.version>17<\/java.version>/g' pom.xml
   sed -i 's/<source>21<\/source>/<source>17<\/source>/g' pom.xml
   sed -i 's/<target>21<\/target>/<target>17<\/target>/g' pom.xml
   ```

2. 构建 Docker 镜像：
   ```bash
   docker build -t bgai:latest .
   ```

### 使用 Docker Compose 快速启动

```bash
docker-compose up -d
```

这将启动以下服务:
- BGAI 应用 (端口 8080)
- Redis (端口 6379)
- Nacos (端口 8848, 9848)

### 自定义配置

您可以在 `docker-compose.yml` 文件中修改以下环境变量:

- `SPRING_PROFILES_ACTIVE`: 应用程序运行的环境 (默认: prod)
- `REDIS_HOST`: Redis 服务器地址
- `REDIS_PORT`: Redis 服务器端口
- `NACOS_HOST`: Nacos 服务器地址
- `NACOS_PORT`: Nacos 服务器端口
- `NACOS_NAMESPACE`: Nacos 命名空间
- `NACOS_GROUP`: Nacos 分组

### API 测试

部署完成后，您可以使用以下方式测试 API:

1. 使用 Postman 导入项目根目录中的 `api-chat-postman-collection.json` 文件
2. 使用项目根目录中的 `test-form-data.html` 或 `test-text-only.html` 文件

API 端点:
- 聊天 API: `http://localhost:8080/Api/chat`
- 表单调试 API: `http://localhost:8080/Api/chat-form-data`

---

## Docker 构建故障排除指南

如果您的 Docker 构建过程非常缓慢或卡住，请按照以下步骤操作：

### 解决方案 1: 使用优化的 Dockerfile 构建

我们提供了一个针对网络和性能问题优化的 Dockerfile：

```bash
# 1. 停止正在运行的构建（如果有）
docker ps -a | grep "build" | awk '{print $1}' | xargs -r docker stop
docker builder prune -f

# 2. 使用优化版本构建
docker build -f Dockerfile.quick -t jiangyang-ai:latest .
```

这个版本做了以下优化：
- 提高了 Maven 构建速度
- 优化了依赖下载
- 减少了不必要的步骤

### 解决方案 2: 离线构建方式

如果您的网络环境非常差，可以使用离线构建方式：

```bash
# 1. 在本地环境构建 JAR 包
./mvnw clean package -DskipTests

# 2. 使用离线 Dockerfile 构建镜像
docker build -f Dockerfile.offline -t jiangyang-ai:latest .
```

### 解决方案 3: 调整 Docker 资源配置

Docker Desktop 资源限制可能导致构建缓慢：

1. 打开 Docker Desktop
2. 点击 "Settings" -> "Resources"
3. 增加 CPU 核心数（最少 4 核）和内存（最少 8GB）
4. 点击 "Apply & Restart"

### 解决方案 4: 使用镜像加速器

配置 Docker 使用国内镜像：

```bash
# 编辑或创建 daemon.json
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": [
    "https://registry.cn-hangzhou.aliyuncs.com",
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com"
  ]
}
EOF

# 重启 Docker
sudo systemctl daemon-reload
sudo systemctl restart docker
```

### 常见问题排查

#### Maven 下载依赖失败

如果主要是 Maven 依赖下载问题，可以尝试：

```bash
# 在本地预先下载依赖
./mvnw dependency:go-offline

# 使用本地 Maven 仓库构建
docker build -f Dockerfile.quick -t jiangyang-ai:latest \
  --build-arg MAVEN_OPTS="-Dmaven.repo.local=./.m2/repository" .
```

#### Docker 构建缓存问题

有时 Docker 构建缓存可能会导致问题：

```bash
# 完全不使用缓存进行构建
docker build -f Dockerfile.quick -t jiangyang-ai:latest --no-cache .
```

#### 网络连接问题

如果是网络连接问题，可尝试：

```bash
# 使用宿主机网络构建
docker build -f Dockerfile.quick -t jiangyang-ai:latest --network=host .
```

---

## 使用远程 Nacos 服务部署指南

### 配置文件说明

本项目包含以下与远程 Nacos 部署相关的文件：

1. **nacos-env.properties** - Nacos 服务器配置文件
2. **docker-compose-remote-nacos.yml** - 简化版 Docker Compose 配置
3. **run-with-remote-nacos.bat/sh** - 快速启动脚本

### 部署步骤

#### 1. 确认远程 Nacos 配置

编辑 `nacos-env.properties` 文件，确保以下配置正确：

```properties
# Nacos 服务器地址
NACOS_HOST=8.133.246.113
NACOS_PORT=8848
NACOS_NAMESPACE=d750d92e-152f-4055-a641-3bc9dda85a29
NACOS_GROUP=DEFAULT_GROUP

# 应用配置
SPRING_PROFILES_ACTIVE=prod
```

#### 2. 使用脚本启动应用

**Windows 环境：**

```bash
run-with-remote-nacos.bat
```

**Linux/Unix 环境：**

```bash
chmod +x run-with-remote-nacos.sh
./run-with-remote-nacos.sh
```

#### 3. 验证部署

应用启动后，可以通过以下方式验证：

1. 检查容器运行状态：
   ```bash
   docker ps
   ```

2. 查看应用日志：
   ```bash
   docker logs bgai-app
   ```

3. 访问应用接口：
   ```
   http://localhost:8080/actuator/health
   ```

4. 登录 Nacos 控制台，确认服务已注册：
   ```
   http://8.133.246.113:8848/nacos/
   ```

---

## API安全和令牌管理

### 令牌过期验证

#### 问题描述

当通过 `/api/auth/refresh-by-userid?userId=<ID>` API 刷新用户的 token 后，使用该用户的旧 token 仍然可以成功调用 `/api/chatGatWay-internal` API，这导致了潜在的安全风险。

具体来说：
1. 当通过 `/api/auth/refresh-by-userid?userId=689258T` 刷新用户 token 后
2. 旧 token 应该被视为无效
3. 但使用旧 token 调用 `/api/chatGatWay-internal` API 仍然成功（服务未重启时）
4. 服务重启后，使用旧 token 调用 `/api/chatGatWay-internal` 会正确报错："Authorization token的用户ID与X-User-Id不匹配"

#### 解决方案

我们对以下几个关键部分进行了修改：

1. `UserServiceImpl.refreshTokenByUserId` 方法：确保删除旧令牌的所有缓存
2. `UserServiceImpl.refreshToken` 方法：增强令牌刷新过程中的缓存清理
3. `UserMapper.findByRefreshToken` 方法：支持通过刷新令牌直接查询用户

### X-User-Id 头部安全增强

#### 问题描述

在使用 `/api/chatGatWay-internal` API 时，仅靠 `X-User-Id` 头部不够安全，需要确保令牌验证的完整性。

#### 安全更新

为了提高系统安全性，我们对 `/api/chatGatWay-internal` 接口的 `X-User-Id` 处理逻辑进行了以下改进：

1. 使用 `X-User-Id` 头部时，**必须同时提供有效的 Authorization 令牌**
2. Authorization 令牌必须是有效的（未过期）
3. 令牌对应的用户ID必须与 `X-User-Id` 值完全匹配
4. 如果不满足上述任何条件，请求将被拒绝（返回 401 或 403 错误）

#### 测试方法

可以使用提供的 `test-x-user-id.html` 测试页面验证安全增强的效果，或使用以下 cURL 命令进行测试：

```bash
curl -X POST http://localhost:8688/api/chatGatWay-internal \
     -H "X-User-Id: 689258T" \
     -H "Authorization: Bearer YOUR_VALID_ACCESS_TOKEN" \
     -F "question=请分析Java中的乐观锁实现方法"
```

---

## OpenAPI 3.0 规范集成

### 升级内容

本项目已将API文档规范从Swagger 2.0升级到OpenAPI 3.0，主要完成了以下工作：

1. 依赖更新
   - 从`springfox-swagger2`和`swagger-bootstrap-ui`切换到`springdoc-openapi-starter-webmvc-ui`和`springdoc-openapi-starter-webflux-ui`
   - 升级到最新版本2.3.0，支持Spring Boot 3.x

2. 配置更新
   - 创建了专门的`OpenApiConfig`配置类，统一管理API文档信息
   - 在`application.yml`中添加了`springdoc`相关配置
   - 配置了API分组，按功能模块组织接口

3. 注解更新
   - 从Swagger 2.0注解迁移到OpenAPI 3.0注解

### 访问方式

API文档现在可以通过以下方式访问：

- **Swagger UI**：http://localhost:8688/swagger-ui.html
- **OpenAPI JSON**：http://localhost:8688/v3/api-docs
- **按分组访问**：http://localhost:8688/swagger-ui.html?urls.primaryName=chat

### Swagger到OpenAPI注解迁移对照表

#### 类级别注解

| Swagger 2.0 | OpenAPI 3.0 | 说明 |
|-------------|-------------|------|
| `@Api(tags = "标签")` | `@Tag(name = "标签", description = "描述")` | 控制器类上的标签注解 |
| `@ApiModel(value = "名称", description = "描述")` | `@Schema(name = "名称", description = "描述")` | 模型类上的注解 |

#### 方法级别注解

| Swagger 2.0 | OpenAPI 3.0 | 说明 |
|-------------|-------------|------|
| `@ApiOperation(value = "操作", notes = "详细说明")` | `@Operation(summary = "操作", description = "详细说明")` | 接口操作描述 |
| `@ApiImplicitParam` | `@Parameter` | 参数描述 |
| `@ApiImplicitParams` | 多个`@Parameter` | 多个参数描述 |
| `@ApiResponses` | `@ApiResponses` | 响应描述容器 |
| `@ApiResponse` | `@ApiResponse` | 响应描述 |

#### 参数级别注解

| Swagger 2.0 | OpenAPI 3.0 | 说明 |
|-------------|-------------|------|
| `@ApiParam(value = "描述", required = true)` | `@Parameter(description = "描述", required = true)` | 参数描述 |
| `@ApiModelProperty(value = "属性描述", required = true)` | `@Schema(description = "属性描述", required = true)` | 模型属性描述 |

### 安全设置

系统配置了两种安全认证方式：

1. **Bearer Token认证**：用于用户登录后的认证
2. **API Key认证**：用于系统间调用的认证

开发者在调用API时需根据接口要求提供相应的认证信息。

---

**Enjoy BGAI！让智能服务更简单！**