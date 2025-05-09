# Docker 构建故障排除指南

如果您的 Docker 构建过程非常缓慢或卡住，请按照以下步骤操作：

## 解决方案 1: 使用优化的 Dockerfile 构建

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

## 解决方案 2: 离线构建方式

如果您的网络环境非常差，可以使用离线构建方式：

```bash
# 1. 在本地环境构建 JAR 包
./mvnw clean package -DskipTests

# 2. 使用离线 Dockerfile 构建镜像
docker build -f Dockerfile.offline -t jiangyang-ai:latest .
```

## 解决方案 3: 调整 Docker 资源配置

Docker Desktop 资源限制可能导致构建缓慢：

1. 打开 Docker Desktop
2. 点击 "Settings" -> "Resources"
3. 增加 CPU 核心数（最少 4 核）和内存（最少 8GB）
4. 点击 "Apply & Restart"

## 解决方案 4: 使用镜像加速器

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

## 解决方案 5: 检查磁盘空间

确保 Docker 有足够的磁盘空间：

```bash
# 查看磁盘使用情况
df -h

# 清理 Docker 资源
docker system prune -a -f --volumes
```

## 常见问题排查

### Maven 下载依赖失败

如果主要是 Maven 依赖下载问题，可以尝试：

```bash
# 在本地预先下载依赖
./mvnw dependency:go-offline

# 使用本地 Maven 仓库构建
docker build -f Dockerfile.quick -t jiangyang-ai:latest \
  --build-arg MAVEN_OPTS="-Dmaven.repo.local=./.m2/repository" .
```

### Docker 构建缓存问题

有时 Docker 构建缓存可能会导致问题：

```bash
# 完全不使用缓存进行构建
docker build -f Dockerfile.quick -t jiangyang-ai:latest --no-cache .
```

### 网络连接问题

如果是网络连接问题，可尝试：

```bash
# 使用宿主机网络构建
docker build -f Dockerfile.quick -t jiangyang-ai:latest --network=host .
```

## 总结

如果您仍然遇到构建问题，可以：

1. 检查日志以获取更具体的错误信息
2. 尝试在不同的网络环境下构建
3. 考虑使用预编译的基础镜像
4. 根据您的环境调整构建策略 