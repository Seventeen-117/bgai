Write-Host "==== BGAI Docker 远程 Nacos 配置助手 ===="
Write-Host "更新 Dockerfile 配置以使用远程 Nacos 服务"
Write-Host ""

# 修复 bootstrap.yml 文件中的配置
Write-Host "更新 bootstrap.yml 配置..."
$bootstrapPath = "src/main/resources/bootstrap.yml"

if (Test-Path $bootstrapPath) {
    $bootstrapContent = Get-Content -Path $bootstrapPath -Raw
    
    # 检查是否已经配置了 local-cache-dir
    if ($bootstrapContent -match "local-cache-dir") {
        # 添加 Nacos 本地缓存目录配置
        $updatedContent = $bootstrapContent -replace "# 配置Nacos本地缓存目录`n        local-cache-dir: /app/nacos/config", "# 使用默认缓存目录，确保容器能正确访问"
        
        # 保存更新后的配置
        $updatedContent | Out-File -FilePath $bootstrapPath -Encoding utf8
        Write-Host "  - bootstrap.yml 已更新：移除了自定义缓存目录配置"
    } else {
        Write-Host "  - bootstrap.yml 无需修改"
    }
} else {
    Write-Host "  - 警告：未找到 bootstrap.yml 文件"
}

# 创建 nacos 环境变量文件
Write-Host "创建 Nacos 环境变量配置文件..."
@"
# Nacos 远程服务器配置
# 将此文件部署在与容器相同的目录中

# Nacos 服务器地址
NACOS_HOST=8.133.246.113
NACOS_PORT=8848
NACOS_NAMESPACE=d750d92e-152f-4055-a641-3bc9dda85a29
NACOS_GROUP=DEFAULT_GROUP

# 应用配置
SPRING_PROFILES_ACTIVE=prod
"@ | Out-File -FilePath "nacos-env.properties" -Encoding utf8

# 创建 Docker Compose 文件，仅包含应用服务
Write-Host "创建简化版 Docker Compose 文件..."
@"
version: '3.8'

services:
  # 应用服务
  bgai-app:
    build:
      context: .
      dockerfile: Dockerfile.fixed
    container_name: bgai-app
    restart: unless-stopped
    ports:
      - "8080:8080"
    env_file:
      - nacos-env.properties
    environment:
      - JAVA_OPTS=-Xms512m -Xmx1g -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=80
    volumes:
      - ./logs:/app/logs
      - ./data:/app/data

# 如果需要本地开发环境，可以取消注释以下服务
#  # Redis service (optional)
#  redis:
#    image: redis:7-alpine
#    container_name: bgai-redis
#    restart: unless-stopped
#    ports:
#      - "6379:6379"
#    command: ["redis-server", "--appendonly", "yes"]
#    volumes:
#      - redis-data:/data
#
#volumes:
#  redis-data:
"@ | Out-File -FilePath "docker-compose-remote-nacos.yml" -Encoding utf8

# 更新 Dockerfile
Write-Host "更新 Dockerfile.fixed..."
$dockerfile = Get-Content -Path "Dockerfile.fixed" -Raw

# 移除 /app/nacos/config 目录创建
$updatedContent = $dockerfile -replace "mkdir -p /app/data /app/logs && \\\r?\n    mkdir -p /app/nacos/config", "mkdir -p /app/data /app/logs"

# 保存更新后的 Dockerfile
$updatedContent | Out-File -FilePath "Dockerfile.fixed" -Encoding utf8

# 创建运行脚本
Write-Host "创建容器启动脚本..."
@"
@echo off
echo ==== BGAI 应用启动 (远程 Nacos 配置) ====
echo 使用远程 Nacos 配置启动应用容器
echo.

rem 构建 Docker 镜像
echo 正在构建 Docker 镜像...
docker-compose -f docker-compose-remote-nacos.yml build

rem 启动容器
echo 正在启动容器...
docker-compose -f docker-compose-remote-nacos.yml up -d

echo.
echo 容器已启动！
echo 应用访问地址: http://localhost:8080
echo 查看日志: docker logs bgai-app
echo.
"@ | Out-File -FilePath "run-with-remote-nacos.bat" -Encoding utf8

Write-Host ""
Write-Host "配置更新完成！已进行以下更改："
Write-Host "1. 更新 bootstrap.yml 移除自定义缓存目录配置"
Write-Host "2. 创建 nacos-env.properties 配置文件用于设置远程 Nacos 参数"
Write-Host "3. 创建 docker-compose-remote-nacos.yml 文件以简化部署"
Write-Host "4. 更新 Dockerfile.fixed 移除不必要的目录创建命令"
Write-Host "5. 创建 run-with-remote-nacos.bat 脚本以快速启动应用"
Write-Host ""
Write-Host "使用方法："
Write-Host "1. 编辑 nacos-env.properties 确保 Nacos 服务器配置正确"
Write-Host "2. 运行 run-with-remote-nacos.bat 启动应用"
Write-Host ""
Write-Host "或手动执行："
Write-Host "docker-compose -f docker-compose-remote-nacos.yml up -d"
Write-Host "" 