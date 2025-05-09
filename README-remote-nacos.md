# BGAI 应用 - 使用远程 Nacos 服务部署指南

本文档提供使用远程 Nacos 服务器部署 BGAI 应用的完整指南。

## 一、概述

BGAI 应用使用 Nacos 作为服务注册和配置中心。在此部署方式中，我们直接连接到已存在的远程 Nacos 服务器，而不是在 Docker 中部署 Nacos 服务。

这种方式的优点：
- 减少容器资源消耗
- 简化部署流程
- 使用已经稳定运行的 Nacos 环境

## 二、配置文件说明

本项目包含以下与远程 Nacos 部署相关的文件：

1. **nacos-env.properties** - Nacos 服务器配置文件
2. **docker-compose-remote-nacos.yml** - 简化版 Docker Compose 配置
3. **run-with-remote-nacos.bat/sh** - 快速启动脚本

## 三、部署步骤

### 1. 确认远程 Nacos 配置

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

### 2. 使用脚本启动应用

#### Windows 环境：

```bash
run-with-remote-nacos.bat
```

#### Linux/Unix 环境：

```bash
chmod +x run-with-remote-nacos.sh
./run-with-remote-nacos.sh
```

### 3. 手动部署

如果不想使用脚本，也可以手动执行以下命令：

```bash
# 构建镜像
docker-compose -f docker-compose-remote-nacos.yml build

# 启动容器
docker-compose -f docker-compose-remote-nacos.yml up -d
```

## 四、验证部署

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
   使用您的 Nacos 账号密码登录，然后检查服务列表中是否有 `bgtech-ai` 服务。

## 五、常见问题

### 1. Nacos 连接错误

如果日志中出现 Nacos 连接错误，可能是以下原因：

- 远程 Nacos 服务器地址配置错误
- 网络问题导致无法连接到 Nacos 服务器
- Nacos 命名空间或分组配置错误

**解决方法**：
- 检查 `nacos-env.properties` 中的配置是否正确
- 确认网络设置，容器是否能访问外部网络
- 使用 `ping` 或 `telnet` 测试与 Nacos 服务器的连接

### 2. 应用启动但无法注册到 Nacos

可能的原因：
- Nacos 服务器防火墙限制
- 命名空间权限问题

**解决方法**：
- 请联系 Nacos 管理员确认服务器访问权限
- 检查应用所用账号是否有命名空间的权限

### 3. 缓存目录错误

如果日志中出现文件系统相关错误，可能是容器内缓存目录权限问题。

**解决方法**：
- 检查 Dockerfile 中目录创建和权限设置是否正确
- 确保容器内 `bgai` 用户有足够权限

## 六、环境变量配置

除了 Nacos 相关配置外，还可以在 `docker-compose-remote-nacos.yml` 中添加其他环境变量：

```yaml
environment:
  - JAVA_OPTS=-Xms512m -Xmx1g -XX:+UseG1GC
  - LOGGING_LEVEL_ROOT=INFO
  - SERVER_PORT=8080
  # 添加其他环境变量
```

## 七、其他资源

- [Spring Cloud Alibaba Nacos 文档](https://spring-cloud-alibaba-group.github.io/github-pages/2021/en-us/index.html#_spring_cloud_alibaba_nacos_discovery)
- [Nacos 官方文档](https://nacos.io/zh-cn/docs/what-is-nacos.html)
- [Docker Compose 文档](https://docs.docker.com/compose/)

## 八、联系与支持

如有部署问题，请联系项目维护人员。 