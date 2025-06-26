# 标准构建Dockerfile - 多阶段构建适用于多种场景
# Multi-stage Dockerfile for Spring Boot application

#######################
# Build stage
#######################
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

# 安装构建工具
RUN apt-get update && apt-get install -y wget \
    && rm -rf /var/lib/apt/lists/*

# Maven优化配置
ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"

# Copy Maven wrapper and pom.xml first for better layer caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make mvnw executable
RUN chmod +x ./mvnw

# Copy application source code
COPY src src

# Build the application
RUN ./mvnw clean package -DskipTests

#######################
# Runtime stage
#######################
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# 安装基础工具和运行时依赖
RUN apt-get update && apt-get install -y \
    wget \
    # 可选: 如需OCR和图像处理，取消下面的注释
    # tesseract-ocr \
    # tesseract-ocr-chi-sim \
    # tesseract-ocr-eng \
    # ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# 创建非root用户
RUN groupadd -r bgai && useradd -r -g bgai bgai

# 创建必要目录并设置权限
RUN mkdir -p /app/data /app/logs /app/config \
    && mkdir -p /app/nacos/config \
    && chown -R bgai:bgai /app

# 复制配置文件
COPY src/main/resources/application-dev.yml /app/config/
COPY src/main/resources/application.yml /app/config/
COPY src/main/resources/bootstrap.yml /app/config/

# 复制Seata配置文件(如果需要分布式事务)
COPY src/main/resources/registry.conf /app/registry.conf
COPY src/main/resources/file.conf /app/file.conf

# 复制JAR包
COPY --from=build /app/target/*.jar app.jar

# 使用非root用户
USER bgai

# 暴露端口
EXPOSE 8688

# 设置环境变量
ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=80"

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -q --spider http://localhost:8688/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.config.additional-location=file:/app/config/ -jar app.jar"]

# 使用说明:
# 1. 标准构建: docker build -t bgai:latest .
# 2. 离线预构建: 
#    a) mvn clean package -DskipTests
#    b) docker build --target runtime -t bgai:offline .
# 3. 开发环境: docker run -p 8688:8688 -e SPRING_PROFILES_ACTIVE=dev bgai:latest
# 4. 配置文件挂载: docker run -v /path/to/config:/app/nacos/config bgai:latest

