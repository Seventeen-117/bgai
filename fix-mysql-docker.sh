#!/bin/bash

echo "==== BGAI Docker Nacos 配置修复助手 ===="
echo "修复 Docker 部署中 Nacos 配置和数据源问题"
echo ""

# 1. 创建一个新的application-docker.yml文件，用于Docker环境
echo "1. 创建Docker专用配置文件..."

mkdir -p src/main/resources

cat > src/main/resources/application-docker.yml << EOF
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
EOF

echo "2. 更新Dockerfile.quick，添加Docker特定配置..."

# 创建临时文件
cat > Dockerfile.quick.new << EOF
# 快速构建版Dockerfile - 优化网络和依赖问题
# 使用与当前系统匹配的JDK版本，避免下载新版本
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

# 设置Maven配置以加速依赖下载
ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"

# 支持本地 Maven 仓库缓存（可选，构建时可用 --build-arg 挂载）
ARG MAVEN_REPO=/root/.m2/repository
RUN mkdir -p /root/.m2 && mkdir -p \${MAVEN_REPO}
VOLUME ["\${MAVEN_REPO}:/root/.m2/repository"]

# 创建Maven settings.xml以使用国内镜像源
RUN echo '<?xml version="1.0" encoding="UTF-8"?>\
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" \
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" \
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">\
  <mirrors>\
    <mirror>\
      <id>aliyun</id>\
      <mirrorOf>central</mirrorOf>\
      <name>Aliyun Maven Central</name>\
      <url>https://maven.aliyun.com/repository/central</url>\
    </mirror>\
    <mirror>\
      <id>aliyun-spring</id>\
      <mirrorOf>spring</mirrorOf>\
      <name>Aliyun Spring</name>\
      <url>https://maven.aliyun.com/repository/spring</url>\
    </mirror>\
    <mirror>\
      <id>aliyun-spring-plugin</id>\
      <mirrorOf>spring-plugin</mirrorOf>\
      <name>Aliyun Spring-plugin</name>\
      <url>https://maven.aliyun.com/repository/spring-plugin</url>\
    </mirror>\
    <mirror>\
      <id>huaweicloud</id>\
      <mirrorOf>*,!aliyun,!aliyun-spring,!aliyun-spring-plugin</mirrorOf>\
      <name>Huawei Cloud</name>\
      <url>https://mirrors.huaweicloud.com/repository/maven/</url>\
    </mirror>\
  </mirrors>\
  <profiles>\
    <profile>\
      <id>defaultProfile</id>\
      <activation>\
        <activeByDefault>true</activeByDefault>\
      </activation>\
      <repositories>\
        <repository>\
          <id>aliyun</id>\
          <url>https://maven.aliyun.com/repository/public</url>\
          <releases>\
            <enabled>true</enabled>\
          </releases>\
          <snapshots>\
            <enabled>true</enabled>\
            <updatePolicy>always</updatePolicy>\
          </snapshots>\
        </repository>\
      </repositories>\
    </profile>\
  </profiles>\
</settings>' > /root/.m2/settings.xml

# 复制pom.xml单独处理依赖，利用Docker缓存机制
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x ./mvnw

# 预下载依赖但跳过测试和构建，增加并行编译参数 -T 1C
RUN ./mvnw dependency:resolve -DskipTests -Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.http.connectTimeout=60000 -Dmaven.wagon.http.readTimeout=60000 -T 1C

# 然后复制源代码并构建
COPY src src
RUN ./mvnw package -DskipTests -Dmaven.test.skip=true -Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.http.connectTimeout=60000 -Dmaven.wagon.http.readTimeout=60000 -T 1C

# 复制 seata 配置文件到镜像，确保分布式事务可用
COPY src/main/resources/registry.conf /app/registry.conf
COPY src/main/resources/file.conf /app/file.conf

# 运行阶段使用最小镜像
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# 安装wget用于健康检查
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

# 复制JAR包
COPY --from=build /app/target/*.jar app.jar

# 创建非root用户
RUN groupadd -r bgai && useradd -r -g bgai bgai
RUN mkdir -p /app/data /app/logs && \
    mkdir -p /app/nacos/config && \
    chown -R bgai:bgai /app

# 使用非root用户运行
USER bgai

# 暴露端口
EXPOSE 8688

# 环境变量
ENV SPRING_PROFILES_ACTIVE=docker \
    TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx1g" \
    NACOS_ENABLED=false \
    NACOS_CONFIG_ENABLED=false

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -q --spider http://localhost:8688/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker", "--spring.cloud.nacos.discovery.enabled=false", "--spring.cloud.nacos.config.enabled=false"]
EOF

# 替换原文件
mv Dockerfile.quick.new Dockerfile.quick

echo "3. 创建docker-compose-mysql.yml文件，包含MySQL服务..."

cat > docker-compose-mysql.yml << EOF
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
EOF

echo "4. 创建启动脚本 run-docker-mysql.sh..."

cat > run-docker-mysql.sh << EOF
#!/bin/bash
echo "启动 BGAI 与 MySQL 服务..."
docker-compose -f docker-compose-mysql.yml up -d

echo "等待MySQL服务准备就绪..."
sleep 10

echo "服务已启动，访问: http://localhost:8688"
EOF

# 设置执行权限
chmod +x run-docker-mysql.sh

echo "5. 创建Windows批处理文件 run-docker-mysql.bat..."

cat > run-docker-mysql.bat << EOF
@echo off
echo 启动 BGAI 与 MySQL 服务...
docker-compose -f docker-compose-mysql.yml up -d

echo 等待MySQL服务准备就绪...
timeout /t 10

echo 服务已启动，访问: http://localhost:8688
EOF

echo ""
echo "修复完成！Docker环境配置已更新，现在您可以使用以下命令构建和运行:"
echo ""
echo "在Linux/Mac上:"
echo "  ./run-docker-mysql.sh"
echo ""
echo "在Windows上:"
echo "  run-docker-mysql.bat"
echo "" 