# BGAI Docker 部署指南

本文档提供了使用 Docker 和 Docker Compose 部署 BGAI 应用的详细说明。

## 系统要求

- Docker 20.10.0 或更高版本
- Docker Compose 2.0.0 或更高版本
- 至少 4GB 内存
- 至少 10GB 磁盘空间

## 技术说明

- 本 Docker 镜像基于 OpenJDK 17 (Eclipse Temurin)
- 应用程序在构建时自动配置为 Java 17 兼容模式
- 适用于 Spring Boot 3.0.x 项目

## 解决 Java 版本问题

如果您在构建时遇到 `invalid target release: 21` 错误，表明您的项目配置为使用 Java 21，但服务器上只有 Java 17。解决此问题有以下几种方式：

### 方法1：使用独立的 Dockerfile

使用项目根目录下的 `Dockerfile.standalone` 文件进行构建：

```bash
docker build -f Dockerfile.standalone -t bgai:latest .
```

### 方法2：使用修复脚本

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

### 方法3：手动修改 pom.xml

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

## 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd bgai
```

### 2. 使用 Docker Compose 启动服务

```bash
docker-compose up -d
```

这将启动以下服务:
- BGAI 应用 (端口 8080)
- Redis (端口 6379)
- Nacos (端口 8848, 9848)

### 3. 查看服务状态

```bash
docker-compose ps
```

### 4. 查看日志

```bash
# 查看应用日志
docker-compose logs -f bgai-app

# 查看特定服务的日志
docker-compose logs -f redis
docker-compose logs -f nacos
```

### 5. 停止服务

```bash
docker-compose down
```

## 自定义配置

### 环境变量

您可以在 `docker-compose.yml` 文件中修改以下环境变量:

- `SPRING_PROFILES_ACTIVE`: 应用程序运行的环境 (默认: prod)
- `REDIS_HOST`: Redis 服务器地址
- `REDIS_PORT`: Redis 服务器端口
- `NACOS_HOST`: Nacos 服务器地址
- `NACOS_PORT`: Nacos 服务器端口
- `NACOS_NAMESPACE`: Nacos 命名空间
- `NACOS_GROUP`: Nacos 分组

### 挂载卷

应用使用以下挂载卷:

- `./logs:/app/logs`: 应用日志目录
- `./data:/app/data`: 应用数据目录
- `redis-data:/data`: Redis 数据
- `nacos-data:/home/nacos/data`: Nacos 数据

## 单独构建和运行 Docker 镜像

如果您想单独构建和运行 BGAI 应用:

```bash
# 构建镜像
docker build -t bgai:latest .

# 运行容器
docker run -d \
  --name bgai-app \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e REDIS_HOST=<redis-host> \
  -e REDIS_PORT=6379 \
  -e NACOS_HOST=<nacos-host> \
  -e NACOS_PORT=8848 \
  -v ./logs:/app/logs \
  -v ./data:/app/data \
  bgai:latest
```

## API 测试

部署完成后，您可以使用以下方式测试 API:

1. 使用 Postman 导入项目根目录中的 `api-chat-postman-collection.json` 文件
2. 使用项目根目录中的 `test-form-data.html` 或 `test-text-only.html` 文件

API 端点:
- 聊天 API: `http://localhost:8080/Api/chat`
- 表单调试 API: `http://localhost:8080/Api/chat-form-data`

## 故障排除

### 应用无法启动

检查日志:
```bash
docker-compose logs bgai-app
```

常见问题:
1. Redis 或 Nacos 连接问题 - 检查网络配置
2. 内存不足 - 增加 Docker 可用内存
3. 权限问题 - 确保挂载卷有正确的权限

### 无法连接到API

1. 检查容器是否运行: `docker-compose ps`
2. 检查8080端口是否暴露: `docker-compose port bgai-app 8080`
3. 检查应用健康检查: `curl http://localhost:8080/actuator/health`

## 安全考虑

1. 默认配置使用了示例密码和令牌，在生产环境中应当更改
2. Nacos 默认启用了认证，但使用了默认令牌
3. 生产环境部署时，应当配置更严格的网络策略和适当的密码 