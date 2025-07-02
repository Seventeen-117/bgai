@echo off
echo 执行CompletelyStandaloneTest测试用例

REM 设置Java 17环境，如果需要的话请调整路径
set JAVA_HOME=%JAVA_17_HOME%
set PATH=%JAVA_HOME%\bin;%PATH%

REM 打印Java版本
echo 当前Java版本:
java -version

REM 设置Maven运行参数
set JAVA_OPTS=-Xmx512m

echo 运行测试...
call .\mvnw test -Dtest=CompletelyStandaloneTest %JAVA_OPTS%

if %errorlevel% neq 0 (
    echo 测试执行失败，请检查错误信息
) else (
    echo 测试执行成功
)

pause 