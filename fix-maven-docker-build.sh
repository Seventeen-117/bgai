#!/bin/bash

echo "==== BGAI Docker构建修复工具 ===="
echo "该脚本将解决Maven依赖下载问题"
echo ""

# 创建maven设置目录
mkdir -p .m2

# 创建settings.xml文件，注意使用正确的XML标签
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

# 创建fixed版本的Dockerfile
cat > Dockerfile.working << 'EOF'
# 使用国内镜像的优化版本Dockerfile
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

# 设置Maven配置以加速依赖下载
ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"

# 复制Maven配置文件
COPY .m2/settings.xml /root/.m2/settings.xml

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

# 运行阶段
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# 创建非root用户
RUN groupadd -r bgai && useradd -r -g bgai bgai
RUN mkdir -p /app/data /app/logs && chown -R bgai:bgai /app

USER bgai
ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx1g"

ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# 清理Docker缓存
echo "清理Docker构建缓存..."
docker builder prune -f

# 显示选项
echo ""
echo "选择构建方式:"
echo "1) 使用修复后的Dockerfile直接构建 (推荐)"
echo "2) 先在本地构建JAR包，然后使用Dockerfile.offline构建"
echo "3) 退出"
echo ""
read -p "请输入选项 [1-3]: " option

case $option in
  1)
    echo "使用修复后的Dockerfile构建..."
    docker build --network=host -f Dockerfile.working -t jiangyang-ai:latest .
    ;;
  2)
    echo "在本地构建JAR包..."
    ./mvnw -s .m2/settings.xml clean package -DskipTests -Dmaven.wagon.http.retryHandler.count=10 -Dmaven.wagon.http.connectTimeout=120000 -Dmaven.wagon.http.readTimeout=120000
    
    if [ $? -eq 0 ]; then
      echo "JAR包构建成功，使用Dockerfile.offline构建Docker镜像..."
      
      # 创建Dockerfile.offline如果不存在
      if [ ! -f Dockerfile.offline ]; then
        cat > Dockerfile.offline << 'EOF'
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY target/*.jar app.jar

RUN groupadd -r bgai && useradd -r -g bgai bgai
RUN mkdir -p /app/data /app/logs && chown -R bgai:bgai /app

USER bgai
ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx1g"

ENTRYPOINT ["java", "-jar", "app.jar"]
EOF
      fi
      
      docker build -f Dockerfile.offline -t jiangyang-ai:latest .
    else
      echo "JAR包构建失败，请检查Maven问题"
    fi
    ;;
  3)
    echo "退出构建"
    exit 0
    ;;
  *)
    echo "无效选项，退出"
    exit 1
    ;;
esac

echo ""
echo "构建过程已完成。如需进一步帮助，请参考docker-build-solution.md" 