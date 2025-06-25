#!/usr/bin/env pwsh

Write-Host "==== BGAI Docker Nacos 配置修复助手 ===="
Write-Host "修复 Docker 部署中 Nacos 配置和数据源问题"
Write-Host ""

# 1. 创建一个新的application-docker.yml文件，用于Docker环境
Write-Host "1. 创建Docker专用配置文件..."

$dockerConfig = @"
server:
  port: 8688
spring:
  datasource:
    dynamic:
      datasource:
        master:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://mysql:3306/bgai?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowPublicKeyRetrieval=true
          username: root
          password: bgai_pass
          type: com.alibaba.druid.pool.DruidDataSource
        slave:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://mysql:3306/bgai?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowPublicKeyRetrieval=true
          username: root
          password: bgai_pass
          type: com.alibaba.druid.pool.DruidDataSource
  cloud:
    nacos:
      discovery:
        enabled: false
      config:
        enabled: false
"@

# 将配置写入文件
$dockerConfig | Out-File -FilePath "src/main/resources/application-docker.yml" -Encoding utf8

Write-Host "2. 更新Dockerfile.quick，添加Docker特定配置..."

$dockerFile = Get-Content -Path "Dockerfile.quick" -Raw

# 更新启动命令，使用Docker特定配置文件
$updatedDockerFile = $dockerFile -replace "ENTRYPOINT \[""java"", ""-jar"", ""app.jar""\]", @"
# 创建数据源配置目录
RUN mkdir -p /app/nacos/config

# 设置环境变量以禁用Nacos
ENV SPRING_PROFILES_ACTIVE=docker \
    NACOS_ENABLED=false \
    NACOS_CONFIG_ENABLED=false

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker", "--spring.cloud.nacos.discovery.enabled=false", "--spring.cloud.nacos.config.enabled=false"]
"@

# 保存更新后的Dockerfile
$updatedDockerFile | Out-File -FilePath "Dockerfile.quick" -Encoding utf8

Write-Host "3. 创建docker-compose-mysql.yml文件，包含MySQL服务..."

$dockerCompose = @"
version: '3'
services:
  bgai:
    build:
      context: .
      dockerfile: Dockerfile.quick
    ports:
      - "8688:8688"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      - mysql
    networks:
      - bgai-network

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: bgai_pass
      MYSQL_DATABASE: bgai
    volumes:
      - ./src/main/resources/sql:/docker-entrypoint-initdb.d
      - mysql-data:/var/lib/mysql
    ports:
      - "3306:3306"
    networks:
      - bgai-network

networks:
  bgai-network:
    driver: bridge

volumes:
  mysql-data:
"@

# 将docker-compose写入文件
$dockerCompose | Out-File -FilePath "docker-compose-mysql.yml" -Encoding utf8

Write-Host "4. 创建启动脚本 run-docker-mysql.sh..."

$startScript = @"
#!/bin/bash
echo "启动 BGAI 与 MySQL 服务..."
docker-compose -f docker-compose-mysql.yml up -d

echo "等待MySQL服务准备就绪..."
sleep 10

echo "服务已启动，访问: http://localhost:8688"
"@

# 将启动脚本写入文件
$startScript | Out-File -FilePath "run-docker-mysql.sh" -Encoding utf8

Write-Host "5. 创建Windows批处理文件 run-docker-mysql.bat..."

$winBatchFile = @"
@echo off
echo 启动 BGAI 与 MySQL 服务...
docker-compose -f docker-compose-mysql.yml up -d

echo 等待MySQL服务准备就绪...
timeout /t 10

echo 服务已启动，访问: http://localhost:8688
"@

# 将Windows批处理写入文件
$winBatchFile | Out-File -FilePath "run-docker-mysql.bat" -Encoding utf8

Write-Host ""
Write-Host "修复完成！Docker环境配置已更新，现在您可以使用以下命令构建和运行:"
Write-Host ""
Write-Host "在Linux/Mac上:"
Write-Host "  chmod +x run-docker-mysql.sh"
Write-Host "  ./run-docker-mysql.sh"
Write-Host ""
Write-Host "在Windows上:"
Write-Host "  run-docker-mysql.bat"
Write-Host "" 