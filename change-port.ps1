Write-Host "==== BGAI 端口修改助手 ===="
Write-Host "将应用端口从8086更改为8080"
Write-Host ""

# 确保src/main/resources/application.yml存在
Write-Host "创建或更新application.yml文件..."
New-Item -ItemType Directory -Force -Path "src/main/resources" | Out-Null
@"
server:
  port: 8080
  
# Additional server configuration
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain
    min-response-size: 2048
  http2:
    enabled: true
"@ | Out-File -FilePath "src/main/resources/application.yml" -Encoding utf8

Write-Host "更新Dockerfiles中的端口设置..."

# 处理Dockerfile和Dockerfile.*文件
Get-ChildItem -Path . -Filter "Dockerfile*" | ForEach-Object {
    $file = $_.FullName
    Write-Host "处理文件: $file"
    
    $content = Get-Content -Path $file -Raw
    
    # 添加wget安装（如果需要）
    if ($content -match "jammy|debian" -and -not ($content -match "apt-get install -y wget")) {
        $content = $content -replace "WORKDIR /app", "WORKDIR /app`n`n# 安装wget用于健康检查`nRUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*"
    }
    
    if ($content -match "alpine" -and -not ($content -match "apk add --no-cache wget")) {
        $content = $content -replace "WORKDIR /app", "WORKDIR /app`n`n# 安装wget用于健康检查`nRUN apk add --no-cache wget"
    }
    
    # 确保暴露8080端口
    if (-not ($content -match "EXPOSE 8080")) {
        $content = $content -replace "USER bgai", "USER bgai`n`n# 暴露端口`nEXPOSE 8080"
    }
    
    # 更新健康检查
    $content = $content -replace "CMD java -version \|\| exit 1", "CMD wget -q --spider http://localhost:8080/actuator/health || exit 1"
    
    # 写回文件
    $content | Out-File -FilePath $file -Encoding utf8
}

Write-Host ""
Write-Host "更新完成！应用现在将使用8080端口。"
Write-Host ""
Write-Host "你可以使用以下命令重新构建Docker镜像:"
Write-Host "  docker build -f Dockerfile.fixed -t jiangyang-ai:latest ."
Write-Host "或者:"
Write-Host "  ./fix-maven-docker-build.sh"
Write-Host "" 