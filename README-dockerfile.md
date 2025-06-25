# Docker 构建指南

本项目提供两个优化的 Dockerfile 选项，满足不同的构建和部署需求：

## 1. 标准构建 (Dockerfile)

标准构建版本提供了一个功能完备、注释清晰的构建过程，适合大多数场景使用。

### 特点：
- 多阶段构建，减小最终镜像大小
- 非 root 用户运行，提高安全性
- 适当的内存和 GC 配置
- 包含健康检查
- 包含 Seata 分布式事务配置

### 使用方法：
```bash
# 标准构建
docker build -t bgai:latest .

# 在开发环境中运行
docker run -p 8688:8688 -e SPRING_PROFILES_ACTIVE=dev bgai:latest

# 挂载配置文件
docker run -v /path/to/config:/app/nacos/config bgai:latest
```

### 离线预构建选项：
```bash
# 1. 先在本地构建 JAR 包
mvn clean package -DskipTests

# 2. 使用预构建的 JAR 创建镜像
docker build --target runtime -t bgai:offline .
```

## 2. 快速构建 (Dockerfile.quick)

针对中国网络环境优化的快速构建版本，特别适合在网络环境不佳的情况下使用。

### 特点：
- 使用多个国内 Maven 镜像源（阿里云、华为云）
- 优化依赖下载策略，增加重试次数和超时时间
- 支持挂载本地 Maven 仓库缓存
- 并行构建优化
- 分阶段优化的构建流程

### 使用方法：
```bash
# 快速构建
docker build -f Dockerfile.quick -t bgai:latest .

# 挂载本地 Maven 仓库加速构建
docker build -f Dockerfile.quick --build-arg MAVEN_REPO=/path/to/m2/repository -t bgai:latest .
```

## 构建选择指南

| 场景 | 推荐选项 |
| --- | --- |
| 正常网络环境 | Dockerfile |
| 首次构建时间长 | Dockerfile.quick |  
| 网络环境不稳定 | Dockerfile.quick |
| 中国大陆网络 | Dockerfile.quick |
| CI/CD 环境 | Dockerfile |
| 本地快速迭代开发 | Dockerfile.quick + 挂载本地 Maven 仓库 |

## Dockerfile 变更说明

我们对项目中的 Dockerfile 进行了整合，将多个特定目的的 Dockerfile 合并为两个主要文件：

1. `Dockerfile` - 合并了原先的 `Dockerfile`、`Dockerfile.standalone`、`Dockerfile.minimal`、`Dockerfile.slim` 的主要功能
2. `Dockerfile.quick` - 专注于中国网络环境优化和构建速度

这种整合简化了项目结构，同时保留了所有必要的构建选项和功能。

## 调整定制项

### 添加 OCR 和视频处理支持

若需要 OCR、图像或视频处理功能，可在 Dockerfile 中取消相关注释：

```dockerfile
# 安装基础工具和运行时依赖
RUN apt-get update && apt-get install -y \
    wget \
    # 可选: 如需OCR和图像处理，取消下面的注释
    tesseract-ocr \
    tesseract-ocr-chi-sim \
    tesseract-ocr-eng \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*
```

### 调整 Java 版本

若需要使用 Java 17 而非 Java 21：

1. 修改 FROM 指令中的 base 镜像版本
2. 可能需要调整 pom.xml 中的 Java 版本配置 