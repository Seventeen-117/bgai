#!/bin/bash

echo "==== BGAI Docker构建助手 ===="
echo "该脚本将帮助处理Maven依赖下载问题并构建Docker镜像"
echo ""

# 清理Docker缓存
echo "清理Docker构建缓存..."
docker builder prune -f

# 创建Dockerfile.fixed文件
echo "创建优化的Dockerfile..."
cat > Dockerfile.fixed << 'EOF'
# 使用国内镜像的优化版本Dockerfile
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

# 设置Maven配置以加速依赖下载
ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"

# 创建Maven配置目录
RUN mkdir -p /root/.m2

# 创建Maven设置文件，使用阿里云镜像源
RUN cat > /root/.m2/settings.xml << 'EOT'
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
      <id>aliyun-spring</id>
      <mirrorOf>spring</mirrorOf>
      <name>Aliyun Spring</name>
      <url>https://maven.aliyun.com/repository/spring</url>
    </mirror>
  </mirrors>
</settings>
EOT

# 复制Maven相关文件
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x ./mvnw

# 预下载依赖，增加超时设置和重试次数
RUN ./mvnw dependency:resolve \
     -DskipTests \
     -Dmaven.wagon.http.retryHandler.count=10 \
     -Dmaven.wagon.http.connectTimeout=120000 \
     -Dmaven.wagon.http.readTimeout=120000

# 复制源代码并构建
COPY src src
RUN ./mvnw package \
    -DskipTests \
    -Dmaven.test.skip=true \
    -Dmaven.wagon.http.retryHandler.count=10 \
    -Dmaven.wagon.http.connectTimeout=120000 \
    -Dmaven.wagon.http.readTimeout=120000

# 运行阶段使用最小镜像
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# 复制JAR包
COPY --from=build /app/target/*.jar app.jar

# 创建非root用户
RUN groupadd -r bgai && useradd -r -g bgai bgai
RUN mkdir -p /app/data /app/logs && chown -R bgai:bgai /app

# 使用非root用户运行
USER bgai

# 环境变量
ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx1g"

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD java -version || exit 1

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# 构建Docker镜像
echo "开始构建Docker镜像..."
docker build --network=host -f Dockerfile.fixed -t jiangyang-ai:latest .

echo ""
echo "构建完成。如果仍然失败，请尝试手动执行以下命令来先构建JAR包："
echo "  ./mvnw clean package -DskipTests"
echo "然后使用Dockerfile.offline来构建镜像："
echo "  docker build -f Dockerfile.offline -t jiangyang-ai:latest ."
echo "" 