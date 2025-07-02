@echo off
echo 使用Java 17执行所有测试

REM 设置Java 17环境，如果需要的话请调整路径
set JAVA_HOME=%JAVA_17_HOME%
set PATH=%JAVA_HOME%\bin;%PATH%

REM 打印Java版本
echo 当前Java版本:
java -version

REM 设置Maven运行参数，移除不兼容参数
set JAVA_OPTS=-Xmx512m -Djdk.attach.allowAttachSelf=true

echo 清理并编译项目...
call .\mvnw clean compile test-compile %JAVA_OPTS%

REM 选择要执行的测试类型
echo 请选择要执行的测试类型:
echo 1. 执行所有测试
echo 2. 执行CompletelyStandaloneTest测试
echo 3. 执行AuthControllerTest测试
echo 4. 执行PriceCacheServiceTest测试
echo 5. 执行SimpleAuthControllerTest测试
set /p choice=请输入选项(1-5): 

if "%choice%"=="1" (
    echo 执行所有测试...
    call .\mvnw test %JAVA_OPTS%
) else if "%choice%"=="2" (
    echo 执行CompletelyStandaloneTest测试...
    call .\mvnw test -Dtest=CompletelyStandaloneTest %JAVA_OPTS%
) else if "%choice%"=="3" (
    echo 执行AuthControllerTest测试...
    call .\mvnw test -Dtest=AuthControllerTest %JAVA_OPTS%
) else if "%choice%"=="4" (
    echo 执行PriceCacheServiceTest测试...
    call .\mvnw test -Dtest=PriceCacheServiceTest %JAVA_OPTS%
) else if "%choice%"=="5" (
    echo 执行SimpleAuthControllerTest测试...
    call .\mvnw test -Dtest=SimpleAuthControllerTest %JAVA_OPTS%
) else (
    echo 无效选项，请重新运行脚本并选择有效选项。
    goto end
)

if %errorlevel% neq 0 (
    echo 测试执行失败，请检查错误信息
) else (
    echo 测试执行成功
)

:end
pause 