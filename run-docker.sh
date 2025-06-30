#!/bin/bash

# 构建Docker镜像
echo "Building Docker image..."
docker build -f Dockerfile.quick -t jiangyang-ai:latest .

# 运行Docker容器
echo "Running Docker container..."
docker run -p 8688:8688 \
  --name jiangyang-ai \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e SEATA_SAGA_STATE_MACHINE_AUTO_REGISTER=false \
  jiangyang-ai:latest

echo "Container started. Access the application at http://localhost:8688"
echo "View logs with: docker logs -f jiangyang-ai" 