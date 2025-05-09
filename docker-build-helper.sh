#!/bin/bash
# 帮助脚本：终止正在进行的Docker构建并使用优化版本重建

# 停止所有正在运行的Docker构建进程
echo "1. 停止当前Docker构建进程..."
docker ps -a | grep "build" | awk '{print $1}' | xargs -r docker stop
docker builder prune -f

# 清理Docker缓存以避免旧缓存导致的问题
echo "2. 清理Docker构建缓存..."
docker system prune -f

# 设置参数以提高构建速度
echo "3. 使用优化的Dockerfile.quick进行构建..."
docker build -f Dockerfile.quick -t jiangyang-ai:latest --no-cache --progress=plain .

echo "构建完成。如果仍然遇到问题，可以尝试以下操作："
echo "- 检查您的Docker资源配置（内存/CPU/存储）"
echo "- 确保网络连接稳定"
echo "- 如还有问题，请尝试离线构建方式" 