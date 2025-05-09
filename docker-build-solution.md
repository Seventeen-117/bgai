# Docker 构建问题解决方案

您遇到的 Docker 构建错误是由于 Maven 无法从中央仓库下载依赖包导致的，具体错误信息：

```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-dependency-plugin:3.3.0:go-offline (default-cli) on project bgai: 
org.eclipse.aether.resolution.DependencyResolutionException: The following artifacts could not be resolved: 
org.apache.maven.surefire:surefire-booter:jar:2.22.2 (absent): 
Could not transfer artifact org.apache.maven.surefire:surefire-booter:jar:2.22.2 from/to central 
(https://repo.maven.apache.org/maven2): Connect to repo.maven.apache.org:443 failed: Connect timed out
```

这是由于网络连接问题导致无法从 Maven 中央仓库下载依赖包。以下提供三种解决方法：

## 解决方案一：使用离线构建方式

最可靠的方式是先在本地构建 JAR 包，然后使用 Dockerfile.offline 进行 Docker 构建：

```bash
# 1. 在本地构建 JAR 包
./mvnw clean package -DskipTests

# 2. 使用 Dockerfile.offline 构建镜像
docker build -f Dockerfile.offline -t jiangyang-ai:latest .
```

## 解决方案二：创建和使用优化版 Dockerfile

1. 创建 `settings.xml` 文件：

```bash
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
```

2. 创建 `Dockerfile.fixed` 文件：

```dockerfile
# 使用国内镜像的优化版本Dockerfile
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

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
```

3. 构建Docker镜像：

```bash
docker build --network=host -f Dockerfile.fixed -t jiangyang-ai:latest .
```

## 解决方案三：使用本地Maven库

如果您的环境中有一个正常工作的Maven仓库，可以直接在Docker构建时使用：

```bash
# 先预下载依赖到本地
./mvnw -s .m2/settings.xml dependency:resolve -DskipTests

# 然后使用挂载本地Maven仓库的方式构建
docker build -f Dockerfile.quick -t jiangyang-ai:latest \
  --build-arg MAVEN_OPTS="-Dmaven.repo.local=$(pwd)/.m2/repository" \
  .
```

## 如果以上方法仍然失败

1. 检查您的网络连接是否稳定
2. 检查是否有代理或防火墙阻止了Maven仓库的访问
3. 可以尝试使用其他网络环境

如有必要，您可以联系网络管理员帮助解决连接问题。 