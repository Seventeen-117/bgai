@echo off
setlocal enabledelayedexpansion

:: BGAI项目综合修复工具 - Windows版本
echo ==============================================
echo         BGAI 项目综合修复工具 v1.0          
echo ==============================================
echo.

:: 颜色设置
set "GREEN=[32m"
set "YELLOW=[33m"
set "RED=[31m"
set "RESET=[0m"

:: 菜单函数
:MENU
echo.
echo %GREEN%请选择要执行的操作：%RESET%
echo 1) 修复Java版本 (Java 21 -^> Java 17)
echo 2) 为测试配置Java环境
echo 3) Docker构建修复 (Maven依赖问题)
echo 4) MySQL Docker配置
echo 5) Nacos Docker配置修复
echo 0) 退出
echo.

set /p choice="请输入选项 [0-5]: "

if "%choice%"=="1" goto FIX_JAVA_VERSION
if "%choice%"=="2" goto FIX_JAVA_FOR_TESTS
if "%choice%"=="3" goto FIX_MAVEN_DOCKER
if "%choice%"=="4" goto FIX_MYSQL_DOCKER
if "%choice%"=="5" goto FIX_NACOS_DOCKER
if "%choice%"=="0" goto EXIT

echo %RED%无效选项，请重新输入%RESET%
goto MENU

:: 功能1: 修复Java版本
:FIX_JAVA_VERSION
echo %GREEN%=== Java 版本修复 (21 -^> 17) ===%RESET%
echo 正在检查当前Java安装...

java -version 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo %RED%警告: 未找到Java!%RESET%
)

echo.
echo %GREEN%=== Maven 版本检查 ===%RESET%
mvn --version 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo 警告: 未找到Maven! 将尝试使用./mvnw
    if exist "mvnw.cmd" (
        call mvnw --version
    ) else (
        echo %RED%错误: 既未找到Maven也未找到mvnw包装器!%RESET%
        goto MENU_RETURN
    )
)

echo.
echo %GREEN%=== 备份原始pom.xml ===%RESET%
if exist "pom.xml" (
    copy pom.xml pom.xml.original
    echo 原始pom.xml已备份到pom.xml.original
) else (
    echo %RED%错误: 当前目录中未找到pom.xml!%RESET%
    goto MENU_RETURN
)

echo.
echo %GREEN%=== 修改pom.xml使用Java 17 ===%RESET%
:: 替换Java版本
powershell -Command "(Get-Content pom.xml) -replace '<java.version>21</java.version>', '<java.version>17</java.version>' | Set-Content pom.xml"
:: 替换编译器源/目标版本
powershell -Command "(Get-Content pom.xml) -replace '<source>21</source>', '<source>17</source>' | Set-Content pom.xml"
powershell -Command "(Get-Content pom.xml) -replace '<target>21</target>', '<target>17</target>' | Set-Content pom.xml"

echo.
echo %GREEN%=== 验证更改 ===%RESET%
:: 检查是否仍有Java 21引用
powershell -Command "if (Select-String -Path pom.xml -Pattern '<java.version>21</java.version>|<source>21</source>|<target>21</target>') { exit 1 } else { exit 0 }"
if %ERRORLEVEL% NEQ 0 (
    echo %RED%警告: pom.xml中仍然存在Java 21引用! 需要手动检查.%RESET%
    powershell -Command "Select-String -Path pom.xml -Pattern 'java.version|source|target'"
) else (
    echo %GREEN%所有Java 21引用已成功更改为Java 17.%RESET%
    powershell -Command "Select-String -Path pom.xml -Pattern 'java.version|source|target'"
)

set /p build_option="是否要构建项目? (y/n): "
if /i "%build_option%"=="y" (
    echo.
    echo %GREEN%=== 构建项目 ===%RESET%
    :: 使用mvnw如果可用，否则使用maven
    if exist "mvnw.cmd" (
        echo 使用mvnw构建...
        call mvnw clean package -DskipTests -Dmaven.compiler.source=17 -Dmaven.compiler.target=17 -Djava.version=17
    ) else (
        echo 使用mvn构建...
        call mvn clean package -DskipTests -Dmaven.compiler.source=17 -Dmaven.compiler.target=17 -Djava.version=17
    )

    if %ERRORLEVEL% EQU 0 (
        echo.
        echo %GREEN%=== 构建成功 ===%RESET%
        echo 项目已成功使用Java 17构建.
        
        if exist "target" (
            echo target目录中的JAR文件:
            dir /b target\*.jar
        )
    ) else (
        echo.
        echo %RED%=== 构建失败 ===%RESET%
        echo 项目构建失败. 请检查上面的错误.
    )
)
goto MENU_RETURN

:: 功能2: 为测试配置Java环境
:FIX_JAVA_FOR_TESTS
echo %GREEN%=== 测试环境Java配置 ===%RESET%

:: 检查环境变量JAVA_HOME_17
if defined JAVA_HOME_17 (
    echo 使用已有的JAVA_HOME_17: %JAVA_HOME_17%
    set "JAVA_HOME=%JAVA_HOME_17%"
) else (
    :: 尝试寻找Java 17
    echo 未找到JAVA_HOME_17环境变量，请提供Java 17安装路径:
    set /p JAVA_HOME="Java 17路径: "
    
    if not exist "%JAVA_HOME%\bin\java.exe" (
        echo %RED%错误: 提供的路径无效或不包含Java可执行文件.%RESET%
        goto MENU_RETURN
    )
)

:: 确认Java版本
echo 当前JAVA_HOME: %JAVA_HOME%
"%JAVA_HOME%\bin\java" -version

:: 临时设置PATH
set "PATH=%JAVA_HOME%\bin;%PATH%"

set /p run_tests="是否要运行测试? (y/n): "
if /i "%run_tests%"=="y" (
    :: 执行Maven测试
    echo 运行测试...
    call mvnw test-compile
    call mvnw test -Dtest=SimpleChatGatewayInternalTest
)

:: 显示配置信息
echo.
echo %GREEN%Java环境已配置为Java 17%RESET%
echo 要在新命令行中使用此配置，请运行:
echo set "JAVA_HOME=%JAVA_HOME%"
echo set "PATH=%%JAVA_HOME%%\bin;%%PATH%%"

goto MENU_RETURN

:: 功能3: Docker构建修复
:FIX_MAVEN_DOCKER
echo %GREEN%=== Docker构建修复工具 ===%RESET%
echo 此功能将解决Maven依赖下载问题

:: 创建maven设置目录
mkdir .m2 2>nul

:: 创建settings.xml文件
echo 创建Maven设置文件...
echo ^<?xml version="1.0" encoding="UTF-8"?^> > .m2\settings.xml
echo ^<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" >> .m2\settings.xml
echo           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" >> .m2\settings.xml
echo           xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd"^> >> .m2\settings.xml
echo   ^<mirrors^> >> .m2\settings.xml
echo     ^<mirror^> >> .m2\settings.xml
echo       ^<id^>aliyun^</id^> >> .m2\settings.xml
echo       ^<mirrorOf^>central^</mirrorOf^> >> .m2\settings.xml
echo       ^<n^>Aliyun Maven Central^</n^> >> .m2\settings.xml
echo       ^<url^>https://maven.aliyun.com/repository/central^</url^> >> .m2\settings.xml
echo     ^</mirror^> >> .m2\settings.xml
echo     ^<mirror^> >> .m2\settings.xml
echo       ^<id^>huaweicloud^</id^> >> .m2\settings.xml
echo       ^<mirrorOf^>*,!aliyun^</mirrorOf^> >> .m2\settings.xml
echo       ^<n^>Huawei Cloud^</n^> >> .m2\settings.xml
echo       ^<url^>https://mirrors.huaweicloud.com/repository/maven/^</url^> >> .m2\settings.xml
echo     ^</mirror^> >> .m2\settings.xml
echo   ^</mirrors^> >> .m2\settings.xml
echo ^</settings^> >> .m2\settings.xml

:: 创建优化的Dockerfile
echo 创建优化的Dockerfile.working...
powershell -Command "Set-Content -Path 'Dockerfile.working' -Value @'
# 使用国内镜像的优化版本Dockerfile
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

# 设置Maven配置以加速依赖下载
ENV MAVEN_OPTS=\"-XX:+TieredCompilation -XX:TieredStopAtLevel=1\"

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
    JAVA_OPTS=\"-Xms512m -Xmx1g\"

ENTRYPOINT [\"java\", \"-jar\", \"app.jar\"]
'@"

:: 清理Docker缓存
echo 清理Docker构建缓存...
docker builder prune -f

:: 显示选项并构建
echo.
echo %YELLOW%选择构建方式:%RESET%
echo 1) 使用修复后的Dockerfile直接构建 (推荐)
echo 2) 先在本地构建JAR包，然后使用Dockerfile.offline构建
echo 3) 取消
echo.
set /p docker_option="请输入选项 [1-3]: "

if "%docker_option%"=="1" (
    echo 使用修复后的Dockerfile构建...
    docker build -f Dockerfile.working -t jiangyang-ai:latest .
) else if "%docker_option%"=="2" (
    echo 在本地构建JAR包...
    call mvnw -s .m2/settings.xml clean package -DskipTests -Dmaven.wagon.http.retryHandler.count=10 -Dmaven.wagon.http.connectTimeout=120000 -Dmaven.wagon.http.readTimeout=120000
    
    if %ERRORLEVEL% EQU 0 (
        echo JAR包构建成功，使用Dockerfile.offline构建Docker镜像...
        
        :: 创建Dockerfile.offline如果不存在
        if not exist "Dockerfile.offline" (
            powershell -Command "Set-Content -Path 'Dockerfile.offline' -Value @'
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY target/*.jar app.jar

RUN groupadd -r bgai && useradd -r -g bgai bgai
RUN mkdir -p /app/data /app/logs && chown -R bgai:bgai /app
RUN mkdir -p /home/bgai/nacos/config && chown -R bgai:bgai /home/bgai

USER bgai
ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=Asia/Shanghai \
    JAVA_OPTS=\"-Xms512m -Xmx1g\"

ENTRYPOINT [\"java\", \"-jar\", \"app.jar\"]
'@"
        )
        
        docker build -f Dockerfile.offline -t jiangyang-ai:latest .
    ) else (
        echo %RED%JAR包构建失败，请检查Maven问题%RESET%
    )
) else (
    echo 取消构建
)

goto MENU_RETURN

:: 功能4: MySQL Docker配置
:FIX_MYSQL_DOCKER
echo %GREEN%=== MySQL Docker配置工具 ===%RESET%
echo 此功能将创建MySQL专用的Docker配置

:: 1. 创建一个新的application-docker.yml文件，用于Docker环境
echo 1. 创建Docker专用配置文件...

mkdir src\main\resources 2>nul

powershell -Command "Set-Content -Path 'src\main\resources\application-docker.yml' -Value @'
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
'@"

:: 2. 创建docker-compose-mysql.yml文件
echo 2. 创建docker-compose-mysql.yml文件...

powershell -Command "Set-Content -Path 'docker-compose-mysql.yml' -Value @'
version: '3'
services:
  bgai:
    build:
      context: .
      dockerfile: Dockerfile.quick
    ports:
      - \"8688:8688\"
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
      - \"3306:3306\"
    networks:
      - bgai-network

networks:
  bgai-network:
    driver: bridge

volumes:
  mysql-data:
'@"

:: 3. 创建启动批处理文件
echo 3. 创建启动批处理文件...

echo @echo off > run-docker-mysql.bat
echo echo 启动 BGAI 与 MySQL 服务... >> run-docker-mysql.bat
echo docker-compose -f docker-compose-mysql.yml up -d >> run-docker-mysql.bat
echo. >> run-docker-mysql.bat
echo echo 等待MySQL服务准备就绪... >> run-docker-mysql.bat
echo timeout /t 10 >> run-docker-mysql.bat
echo. >> run-docker-mysql.bat
echo echo 服务已启动，访问: http://localhost:8688 >> run-docker-mysql.bat

echo %GREEN%MySQL Docker配置已完成!%RESET%
echo 要启动服务，请运行: run-docker-mysql.bat

goto MENU_RETURN

:: 功能5: Nacos Docker配置修复
:FIX_NACOS_DOCKER
echo %GREEN%=== Nacos Docker配置修复 ===%RESET%

powershell -Command "
$files = Get-ChildItem -Path . -Filter 'Dockerfile*'
foreach ($file in $files) {
    Write-Host \"处理文件: $($file.Name)\"
    $content = Get-Content -Path $file.FullName -Raw
    
    if (-not ($content -match '/home/bgai/nacos/config')) {
        $content = $content -replace 'RUN mkdir -p /app/data /app/logs && chown -R bgai:bgai /app', 'RUN mkdir -p /app/data /app/logs && \
    mkdir -p /home/bgai/nacos/config && \
    chown -R bgai:bgai /app && \
    chown -R bgai:bgai /home/bgai'
        
        $content | Set-Content -Path $file.FullName
    }
}
"

echo %GREEN%Nacos配置目录修复完成!%RESET%
echo 所有Dockerfile已更新，添加了创建Nacos配置目录的命令

goto MENU_RETURN

:: 返回菜单
:MENU_RETURN
echo.
echo 按任意键返回主菜单...
pause >nul
goto MENU

:EXIT
echo %GREEN%感谢使用BGAI项目修复工具!%RESET%
exit /b 0 