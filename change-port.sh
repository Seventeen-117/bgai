#!/bin/bash

echo "==== BGAI 端口修改助手 ===="
echo "将应用端口从8086更改为8080"
echo ""

# 确保src/main/resources/application.yml存在
echo "创建或更新application.yml文件..."
mkdir -p src/main/resources
cat > src/main/resources/application.yml << 'EOF'
server:
  port: 8080
  
# Additional server configuration
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain
    min-response-size: 2048
  http2:
    enabled: true
EOF

echo "更新Dockerfile健康检查..."
# 更新所有Dockerfile的健康检查方式
for dockerfile in Dockerfile Dockerfile.* ; do
  if [ -f "$dockerfile" ]; then
    # 检查是否已经包含wget安装
    if ! grep -q "apt-get install -y wget" "$dockerfile"; then
      # 如果在jammy或debian基础上，添加wget安装
      if grep -q "jammy\|debian" "$dockerfile"; then
        sed -i '/WORKDIR \/app/a\\\n# 安装wget用于健康检查\nRUN apt-get update \&\& apt-get install -y wget \&\& rm -rf /var/lib/apt/lists/*' "$dockerfile"
      fi
      # 如果在alpine基础上，使用不同的安装命令
      if grep -q "alpine" "$dockerfile"; then
        sed -i '/WORKDIR \/app/a\\\n# 安装wget用于健康检查\nRUN apk add --no-cache wget' "$dockerfile"
      fi
    fi
    
    # 确保暴露8080端口
    if ! grep -q "EXPOSE 8080" "$dockerfile"; then
      sed -i '/USER bgai/a\\\n# 暴露端口\nEXPOSE 8080' "$dockerfile"
    fi
    
    # 更新健康检查方式
    sed -i 's|CMD java -version || exit 1|CMD wget -q --spider http://localhost:8080/actuator/health || exit 1|g' "$dockerfile"
  fi
done

echo "更新完成！应用现在将使用8080端口。"
echo ""
echo "你可以使用以下命令重新构建Docker镜像:"
echo "  docker build -f Dockerfile.fixed -t jiangyang-ai:latest ."
echo "或者:"
echo "  ./fix-maven-docker-build.sh"
echo "" 