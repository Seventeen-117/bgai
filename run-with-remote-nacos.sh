#!/bin/bash

echo "==== BGAI 应用启动 (远程 Nacos 配置) ===="
echo "使用远程 Nacos 配置启动应用容器"
echo ""

# 构建 Docker 镜像
echo "正在构建 Docker 镜像..."
docker-compose -f docker-compose-remote-nacos.yml build

# 启动容器
echo "正在启动容器..."
docker-compose -f docker-compose-remote-nacos.yml up -d

echo ""
echo "容器已启动！"
echo "应用访问地址: http://localhost:8080"
echo "查看日志: docker logs bgai-app"
echo "" 