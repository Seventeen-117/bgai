Write-Host "==== BGAI Nacos 配置修复助手 ===="
Write-Host "修复 Docker 部署中 Nacos 配置目录的问题"
Write-Host ""

Write-Host "更新 Dockerfile.fixed..."
$dockerfile = Get-Content -Path "Dockerfile.fixed" -Raw

# 在创建 bgai 用户和目录的代码后添加创建 Nacos 配置目录的命令
$updatedContent = $dockerfile -replace "RUN mkdir -p /app/data /app/logs && chown -R bgai:bgai /app", @"
RUN mkdir -p /app/data /app/logs && \
    mkdir -p /home/bgai/nacos/config && \
    chown -R bgai:bgai /app && \
    chown -R bgai:bgai /home/bgai
"@

# 保存更新后的 Dockerfile
$updatedContent | Out-File -FilePath "Dockerfile.fixed" -Encoding utf8

Write-Host "更新 Dockerfile.quick..."
$dockerfile = Get-Content -Path "Dockerfile.quick" -Raw

# 在创建 bgai 用户和目录的代码后添加创建 Nacos 配置目录的命令
$updatedContent = $dockerfile -replace "RUN mkdir -p /app/data /app/logs && chown -R bgai:bgai /app", @"
RUN mkdir -p /app/data /app/logs && \
    mkdir -p /home/bgai/nacos/config && \
    chown -R bgai:bgai /app && \
    chown -R bgai:bgai /home/bgai
"@

# 保存更新后的 Dockerfile
$updatedContent | Out-File -FilePath "Dockerfile.quick" -Encoding utf8

# 处理所有其他 Dockerfile 文件
Get-ChildItem -Path . -Filter "Dockerfile*" -Exclude "Dockerfile.fixed", "Dockerfile.quick" | ForEach-Object {
    $file = $_.FullName
    Write-Host "处理文件: $file"
    
    $content = Get-Content -Path $file -Raw
    
    # 检查是否已经包含 nacos/config 目录
    if (-not ($content -match "/home/bgai/nacos/config")) {
        # 添加创建 Nacos 配置目录的命令
        $updatedContent = $content -replace "mkdir -p /app/data /app/logs", "mkdir -p /app/data /app/logs && mkdir -p /home/bgai/nacos/config"
        $updatedContent = $updatedContent -replace "chown -R bgai:bgai /app", "chown -R bgai:bgai /app && chown -R bgai:bgai /home/bgai"
        
        # 保存更新后的文件
        $updatedContent | Out-File -FilePath $file -Encoding utf8
    }
}

Write-Host ""
Write-Host "修复完成！所有 Dockerfile 已更新，添加了创建 Nacos 配置目录的命令。"
Write-Host ""
Write-Host "你可以使用以下命令重新构建 Docker 镜像:"
Write-Host "  docker build -f Dockerfile.fixed -t jiangyang-ai:latest ."
Write-Host ""
Write-Host "如果仍然有问题，请考虑在 bootstrap.yml 中配置 Nacos 客户端以使用不同的缓存目录:"
Write-Host "spring.cloud.nacos.config.local-cache-dir=\${user.home}/nacos/config"
Write-Host "" 