#!/bin/bash

# BGAI项目综合修复工具
# 此脚本合并了各种fix-*.sh脚本的功能

echo "============================================="
echo "        BGAI 项目综合修复工具 v1.0          "
echo "============================================="
echo ""

# 创建颜色函数
print_green() {
    echo -e "\e[32m$1\e[0m"
}

print_yellow() {
    echo -e "\e[33m$1\e[0m"
}

print_red() {
    echo -e "\e[31m$1\e[0m"
}

# 选择菜单
show_menu() {
    echo ""
    print_green "请选择要执行的操作："
    echo "1) 修复Java版本 (Java 21 -> Java 17)"
    echo "2) 为测试配置Java环境"
    echo "3) Docker构建修复 (Maven依赖问题)"
    echo "4) MySQL Docker配置"
    echo "5) Nacos Docker配置修复"
    echo "0) 退出"
    echo ""
}

# 功能1: 修复Java版本
fix_java_version() {
    print_green "=== Java 版本修复 (21 -> 17) ==="
    echo "正在检查当前Java安装..."

    if command -v java &>/dev/null; then
        java -version
    else
        print_red "警告: 未找到Java!"
    fi

    echo -e "\n=== Maven 版本检查 ==="
    if command -v mvn &>/dev/null; then
        mvn --version
    else
        echo "警告: 未找到Maven! 将尝试使用./mvnw"
        if [ -f "./mvnw" ]; then
            chmod +x ./mvnw
            ./mvnw --version
        else
            print_red "错误: 既未找到Maven也未找到mvnw包装器!"
            return 1
        fi
    fi

    echo -e "\n=== 备份原始pom.xml ==="
    if [ -f "pom.xml" ]; then
        cp pom.xml pom.xml.original
        echo "原始pom.xml已备份到pom.xml.original"
    else
        print_red "错误: 当前目录中未找到pom.xml!"
        return 1
    fi

    echo -e "\n=== 修改pom.xml使用Java 17 ==="
    # 替换Java版本
    sed -i 's/<java.version>21<\/java.version>/<java.version>17<\/java.version>/g' pom.xml
    # 替换编译器源/目标版本
    sed -i 's/<source>21<\/source>/<source>17<\/source>/g' pom.xml
    sed -i 's/<target>21<\/target>/<target>17<\/target>/g' pom.xml

    echo -e "\n=== 验证更改 ==="
    # 检查是否仍有Java 21引用
    if grep -q "<java.version>21</java.version>" pom.xml || grep -q "<source>21</source>" pom.xml || grep -q "<target>21</target>" pom.xml; then
        print_red "警告: pom.xml中仍然存在Java 21引用! 需要手动检查."
        grep -n "java.version\|source\|target" pom.xml
    else
        print_green "所有Java 21引用已成功更改为Java 17."
        grep -n "java.version\|source\|target" pom.xml
    fi

    read -p "是否要构建项目? (y/n): " build_option
    if [ "$build_option" = "y" ] || [ "$build_option" = "Y" ]; then
        echo -e "\n=== 构建项目 ==="
        # 使用mvnw如果可用，否则使用maven
        if [ -f "./mvnw" ]; then
            echo "使用mvnw构建..."
            ./mvnw clean package -DskipTests -Dmaven.compiler.source=17 -Dmaven.compiler.target=17 -Djava.version=17
        else
            echo "使用mvn构建..."
            mvn clean package -DskipTests -Dmaven.compiler.source=17 -Dmaven.compiler.target=17 -Djava.version=17
        fi

        if [ $? -eq 0 ]; then
            print_green "\n=== 构建成功 ==="
            echo "项目已成功使用Java 17构建."
            
            if [ -d "./target" ]; then
                echo "target目录中的JAR文件:"
                ls -la ./target/*.jar
            fi
        else
            print_red "\n=== 构建失败 ==="
            echo "项目构建失败. 请检查上面的错误."
        fi
    fi
}

# 功能2: 为测试配置Java环境
fix_java_for_tests() {
    print_green "=== 测试环境Java配置 ==="

    # 检查是否设置了JAVA_HOME_17环境变量
    if [ ! -z "$JAVA_HOME_17" ]; then
        echo "使用已有的JAVA_HOME_17: $JAVA_HOME_17"
        export JAVA_HOME=$JAVA_HOME_17
    else
        # 尝试常见的Java安装路径
        for location in "/usr/lib/jvm/java-17-openjdk" "/usr/lib/jvm/java-17-oracle" "/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home"; do
            if [ -d "$location" ]; then
                echo "找到Java 17: $location"
                export JAVA_HOME=$location
                break
            fi
        done
        
        # 如果仍未找到Java 17，请求用户输入路径
        if [ -z "$JAVA_HOME" ] || [ ! -d "$JAVA_HOME" ]; then
            print_yellow "未找到Java 17安装. 请提供Java 17安装路径:"
            read -p "Java 17路径: " JAVA_HOME
            
            if [ ! -d "$JAVA_HOME" ] || [ ! -f "$JAVA_HOME/bin/java" ]; then
                print_red "错误: 提供的路径无效或不包含Java可执行文件."
                return 1
            fi
        fi
    fi

    # 确认Java版本
    echo "当前JAVA_HOME: $JAVA_HOME"
    "$JAVA_HOME/bin/java" -version

    # 临时设置PATH
    export PATH=$JAVA_HOME/bin:$PATH

    read -p "是否要运行测试? (y/n): " run_tests
    if [ "$run_tests" = "y" ] || [ "$run_tests" = "Y" ]; then
        # 执行Maven测试
        echo "运行测试..."
        ./mvnw test-compile
        ./mvnw test -Dtest=SimpleChatGatewayInternalTest
    fi

    # 显示配置信息
    echo ""
    print_green "Java环境已配置为Java 17"
    echo "要在新终端中使用此配置，请运行:"
    echo "export JAVA_HOME=$JAVA_HOME"
    echo "export PATH=\$JAVA_HOME/bin:\$PATH"
}

# 功能3: Docker构建修复
fix_maven_docker_build() {
    print_green "=== Docker构建修复工具 ==="
    echo "此功能将解决Maven依赖下载问题"

    # 创建maven设置目录
    mkdir -p .m2

    # 创建settings.xml文件
    echo "创建Maven设置文件..."
    cat > .m2/settings.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" 
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <n>Aliyun Maven Central</n>
      <url>https://maven.aliyun.com/repository/central</url>
    </mirror>
    <mirror>
      <id>huaweicloud</id>
      <mirrorOf>*,!aliyun</mirrorOf>
      <n>Huawei Cloud</n>
      <url>https://mirrors.huaweicloud.com/repository/maven/</url>
    </mirror>
  </mirrors>
</settings>
EOF

    # 创建优化的Dockerfile
    echo "创建优化的Dockerfile..."
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
RUN mkdir -p /home/bgai/nacos/config && chown -R bgai:bgai /home/bgai

USER bgai
ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx1g"

ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

    # 清理Docker缓存
    echo "清理Docker构建缓存..."
    docker builder prune -f

    # 显示选项并构建
    echo ""
    print_yellow "选择构建方式:"
    echo "1) 使用修复后的Dockerfile直接构建 (推荐)"
    echo "2) 先在本地构建JAR包，然后使用Dockerfile.offline构建"
    echo "3) 取消"
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
RUN mkdir -p /home/bgai/nacos/config && chown -R bgai:bgai /home/bgai

USER bgai
ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx1g"

ENTRYPOINT ["java", "-jar", "app.jar"]
EOF
          fi
          
          docker build -f Dockerfile.offline -t jiangyang-ai:latest .
        else
          print_red "JAR包构建失败，请检查Maven问题"
        fi
        ;;
      3)
        echo "取消构建"
        ;;
      *)
        print_red "无效选项"
        ;;
    esac
}

# 功能4: MySQL Docker配置
fix_mysql_docker() {
    print_green "=== MySQL Docker配置工具 ==="
    echo "此功能将创建MySQL专用的Docker配置"

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

    # 2. 创建docker-compose-mysql.yml文件
    echo "2. 创建docker-compose-mysql.yml文件..."

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

    # 3. 创建启动脚本
    echo "3. 创建启动脚本..."

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

    print_green "MySQL Docker配置已完成!"
    echo "要启动服务，请运行: ./run-docker-mysql.sh"
}

# 功能5: Nacos Docker配置修复
fix_nacos_docker() {
    print_green "=== Nacos Docker配置修复 ==="
    
    # 处理所有Dockerfile文件
    for dockerfile in Dockerfile Dockerfile.*; do
        if [ -f "$dockerfile" ]; then
            echo "处理文件: $dockerfile"
            
            # 创建临时文件
            awk '{
                if ($0 ~ /mkdir -p \/app\/data \/app\/logs/ && $0 !~ /\/home\/bgai\/nacos\/config/) {
                    print $0 " && \\\n    mkdir -p /home/bgai/nacos/config && \\\n    chown -R bgai:bgai /app && \\\n    chown -R bgai:bgai /home/bgai"
                } else {
                    print $0
                }
            }' $dockerfile > ${dockerfile}.tmp
            
            # 替换原文件
            mv ${dockerfile}.tmp $dockerfile
        fi
    done
    
    print_green "Nacos配置目录修复完成!"
    echo "所有Dockerfile已更新，添加了创建Nacos配置目录的命令"
}

# 主功能循环
while true; do
    show_menu
    read -p "请输入选项 [0-5]: " choice
    
    case $choice in
        1) fix_java_version ;;
        2) fix_java_for_tests ;;
        3) fix_maven_docker_build ;;
        4) fix_mysql_docker ;;
        5) fix_nacos_docker ;;
        0) 
            print_green "感谢使用BGAI项目修复工具!"
            exit 0 
            ;;
        *) print_red "无效选项，请重新输入" ;;
    esac
    
    echo ""
    read -p "按Enter键继续..."
done 