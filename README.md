# BGAI 项目说明

## 项目简介

BGAI 是一个基于 Spring Boot 3、Spring Cloud Alibaba、Seata 分布式事务、MyBatis-Plus、RocketMQ、Elasticsearch、Redis、Druid 动态数据源、Pinia+Vue3 前后端分离的智能 AI 服务平台。支持多数据源、分布式事务、微服务注册与配置、消息队列、缓存、全文检索等企业级能力。

---

## 目录结构

```
bgai/
├── src/
│   ├── main/
│   │   ├── java/com/bgpay/bgai/    # 后端主代码（controller, service, entity, config, ...）
│   │   └── resources/              # 配置文件、SQL、Saga、Mapper等
│   └── test/                       # 后端测试代码
├── frontend/                       # 前端 Vue3 + Vite + Pinia 项目
├── Dockerfile*                     # 多种 Docker 构建方案
├── docker-compose.yml              # 一键启动后端及依赖服务
├── README.md                       # 项目说明
└── ...                             # 其他脚本、文档、工具
```

---

## 技术栈

### 后端

- **Spring Boot 3.2.5**
- **Spring Cloud 2023.0.1** + **Spring Cloud Alibaba 2022.0.0.0**
- **Seata 1.7.0**（分布式事务，自动代理数据源）
- **MyBatis-Plus 3.5.5**（高效 ORM）
- **Druid 1.2.8**（多数据源、SQL监控）
- **RocketMQ 4.9.4/2.2.3**（消息队列）
- **Elasticsearch 8.12.2**（全文检索）
- **Redis/Redisson 3.24.3**（缓存/分布式锁）
- **Caffeine**（本地缓存）
- **Nacos**（注册中心/配置中心）
- **多种分布式中间件集成**

### 前端

- **Vue 3.3.8**
- **Vite 5**
- **Pinia 2.1.7**（状态管理）
- **Vue Router 4.2.5**
- **Axios**（HTTP请求）
- **ESLint**（代码规范）

---

## 快速启动

### 1. 环境准备

- JDK 21
- Maven 3.8+
- Node.js 18+
- Docker & Docker Compose（推荐一键启动依赖服务）

### 2. 启动依赖服务（推荐）

```bash
docker-compose up -d
```
> 包含 MySQL、Redis、Nacos、RocketMQ、Elasticsearch、Seata-Server 等

### 3. 启动后端

```bash
# 编译
./mvnw clean package -DskipTests

# 启动
java -jar target/bgai-0.0.1-SNAPSHOT.jar
# 或
./start.sh
```

#### 本地开发（热部署）

```bash
./mvnw spring-boot:run
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```
访问：http://localhost:5173

---

## 主要功能

- **多数据源动态路由**（Druid + 自定义 DynamicDataSource）
- **分布式事务**（Seata 自动代理，无需手动注册）
- **微服务注册/配置**（Nacos）
- **消息队列**（RocketMQ）
- **缓存/分布式锁**（Redis/Redisson/Caffeine）
- **全文检索**（Elasticsearch）
- **Saga 状态机**（支持复杂业务编排）
- **前后端分离**（Vue3 + Vite + Pinia）
- **丰富的脚本和 Docker 支持**

---

## 重要约定与最佳实践

- **数据源代理**：所有物理数据源（master/slave）由 Seata 自动代理，dynamicDataSource 只做路由。
- **分布式事务**：只需在业务方法上加 `@GlobalTransactional`，无需手动注册 ResourceManager。
- **配置管理**：所有环境变量、数据库连接、MQ等均可通过 Nacos 配置中心集中管理。
- **前端开发**：推荐使用 VSCode + Volar 插件，支持 TypeScript/JSX/Vue3 语法高亮和类型检查。

---

## 常见问题

### 1. 分布式事务不生效/报 DataSourceProxy 错

- 不要自定义 Seata ResourceManager，全部交由 Seata 官方自动注册。
- master/slave 只返回 DruidDataSource，dynamicDataSource 只做路由。
- application.yml 必须有 `seata.enable-auto-data-source-proxy: true`。

### 2. Nacos/Seata/Redis/ES 启动失败

- 检查端口冲突、内存限制、配置文件路径。
- 可用 `docker-compose logs` 查看详细日志。

### 3. 前端无法访问后端接口

- 检查后端端口、CORS 配置、Nginx 代理设置。

---

## 目录与脚本说明

- `start.sh`/`start.bat`：一键启动后端
- `build-docker-fixed.sh`：构建生产镜像
- `docker-compose.yml`：一键启动所有依赖服务
- `frontend/`：前端源码，支持热更新
- `src/main/resources/application.yml`：主配置文件
- `README-build-troubleshooting.md`：常见构建/部署问题解决

---

## 贡献与协作

1. Fork 本仓库，创建 feature 分支
2. 提交 PR，描述变更内容
3. 代码需通过 CI 检查和单元测试

---

## 联系与支持

- 技术支持：请提 Issue 或联系项目维护者
- 文档完善、Bug 反馈、Feature 需求欢迎随时提交

---

**Enjoy BGAI！让智能服务更简单！**