#!/bin/bash

echo "==== BGAI Docker构建助手 ===="
echo "该脚本将帮助处理Maven依赖下载问题并构建Docker镜像"
echo ""

# 清理Docker缓存
echo "清理Docker构建缓存..."
docker builder prune -f

# 创建临时Maven设置文件
echo "配置Maven国内镜像..."
mkdir -p .m2
cat > .m2/settings.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" 
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <name>Aliyun Maven Central</name>
      <url>https://maven.aliyun.com/repository/central</url>
    </mirror>
    <mirror>
      <id>huaweicloud</id>
      <mirrorOf>*,!aliyun</mirrorOf>
      <name>Huawei Cloud</name>
      <url>https://mirrors.huaweicloud.com/repository/maven/</url>
    </mirror>
  </mirrors>
</settings>
EOF

# 预下载Maven依赖到本地
echo "预下载Maven依赖（这可能需要几分钟时间）..."
./mvnw -s .m2/settings.xml dependency:resolve -DskipTests -Dmaven.wagon.http.retryHandler.count=10 -Dmaven.wagon.http.connectTimeout=120000 -Dmaven.wagon.http.readTimeout=120000

# 确保mvnw是可执行的
chmod +x ./mvnw

# 构建最终镜像
echo "构建Docker镜像..."
docker build --network=host \
  -f Dockerfile.fixed \
  --build-arg MAVEN_OPTS="-Dmaven.repo.local=./.m2/repository -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true" \
  -t jiangyang-ai:latest \
  .

echo ""
echo "构建完成。如果仍然失败，请尝试执行："
echo "  ./mvnw -s .m2/settings.xml clean package -DskipTests"
echo "然后使用Dockerfile.offline来构建镜像："
echo "  docker build -f Dockerfile.offline -t jiangyang-ai:latest ."
echo "" 