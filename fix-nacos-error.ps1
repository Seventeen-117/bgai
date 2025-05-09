Write-Host "==== BGAI Nacos 配置错误修复助手 ===="
Write-Host "修复 Docker 部署中 Nacos 配置目录相关的问题"
Write-Host ""

# 修复 bootstrap.yml 文件
Write-Host "修改 bootstrap.yml 配置..."
$bootstrapPath = "src/main/resources/bootstrap.yml"

if (Test-Path $bootstrapPath) {
    $bootstrapContent = Get-Content -Path $bootstrapPath -Raw
    
    # 检查是否已经配置了 local-cache-dir
    if (-not ($bootstrapContent -match "local-cache-dir")) {
        # 添加 Nacos 本地缓存目录配置
        $updatedContent = $bootstrapContent -replace "file-extension: yaml", "file-extension: yaml`n        # 配置Nacos本地缓存目录`n        local-cache-dir: /app/nacos/config"
        
        # 保存更新后的配置
        $updatedContent | Out-File -FilePath $bootstrapPath -Encoding utf8
        Write-Host "  - bootstrap.yml 更新完成：添加了 Nacos 本地缓存目录配置"
    } else {
        Write-Host "  - bootstrap.yml 已包含 Nacos 缓存目录配置，无需修改"
    }
} else {
    Write-Host "  - 警告：未找到 bootstrap.yml 文件"
}

# 处理所有 Dockerfile 文件
Write-Host "更新所有 Dockerfile 文件..."
Get-ChildItem -Path . -Filter "Dockerfile*" | ForEach-Object {
    $file = $_.FullName
    $fileName = $_.Name
    Write-Host "  处理文件: $fileName"
    
    $content = Get-Content -Path $file -Raw
    
    # 检查是否已经包含 nacos/config 目录
    if (-not ($content -match "/app/nacos/config")) {
        # 添加创建 Nacos 配置目录的命令
        $updatedContent = $content -replace "mkdir -p /app/data /app/logs", "mkdir -p /app/data /app/logs && mkdir -p /app/nacos/config"
        $updatedContent = $updatedContent -replace "chown -R bgai:bgai /app", "chown -R bgai:bgai /app"
        
        # 保存更新后的文件
        $updatedContent | Out-File -FilePath $file -Encoding utf8
        Write-Host "    - 已添加 Nacos 缓存目录"
    } else {
        Write-Host "    - 文件已包含 Nacos 缓存目录，无需修改"
    }
}

# 创建修复镜像构建脚本
Write-Host "创建修复后的构建脚本..."
@"
#!/bin/bash

echo "==== BGAI Docker 构建 (含 Nacos 修复) ===="
echo "构建修复了 Nacos 配置目录问题的 Docker 镜像"
echo ""

# 清理 Docker 缓存
echo "清理 Docker 构建缓存..."
docker builder prune -f

# 构建 Docker 镜像
echo "开始构建 Docker 镜像..."
docker build --network=host -f Dockerfile.fixed -t jiangyang-ai:latest .

echo ""
echo "构建完成！现在可以使用以下命令运行容器："
echo "  docker run -p 8080:8080 jiangyang-ai:latest"
echo ""
"@ | Out-File -FilePath "build-docker-fixed.sh" -Encoding utf8

# 创建 Windows 批处理文件版本
@"
@echo off
echo ==== BGAI Docker 构建 (含 Nacos 修复) ====
echo 构建修复了 Nacos 配置目录问题的 Docker 镜像
echo.

rem 清理 Docker 缓存
echo 清理 Docker 构建缓存...
docker builder prune -f

rem 构建 Docker 镜像
echo 开始构建 Docker 镜像...
docker build --network=host -f Dockerfile.fixed -t jiangyang-ai:latest .

echo.
echo 构建完成！现在可以使用以下命令运行容器：
echo   docker run -p 8080:8080 jiangyang-ai:latest
echo.
"@ | Out-File -FilePath "build-docker-fixed.bat" -Encoding utf8

Write-Host ""
Write-Host "修复完成！已进行以下更改："
Write-Host "1. 修改 bootstrap.yml 设置 Nacos 本地缓存目录为 /app/nacos/config"
Write-Host "2. 更新所有 Dockerfile，添加创建 /app/nacos/config 目录的命令"
Write-Host "3. 创建了修复后的构建脚本 (build-docker-fixed.sh 和 build-docker-fixed.bat)"
Write-Host ""
Write-Host "你可以使用以下命令重新构建 Docker 镜像:"
Write-Host "  docker build -f Dockerfile.fixed -t jiangyang-ai:latest ."
Write-Host "或者运行创建的脚本:"
Write-Host "  ./build-docker-fixed.sh (Linux/macOS)"
Write-Host "  build-docker-fixed.bat (Windows)"
Write-Host "" 