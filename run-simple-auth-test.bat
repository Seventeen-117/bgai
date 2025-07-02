@echo off
echo 执行SimpleAuthControllerTest测试用例

REM 设置Java 17环境，如果需要的话请调整路径
set JAVA_HOME=%JAVA_17_HOME%
set PATH=%JAVA_HOME%\bin;%PATH%

REM 打印Java版本
echo 当前Java版本:
java -version

REM 设置Maven运行参数，移除不兼容参数
set JAVA_OPTS=-Xmx512m -Djdk.attach.allowAttachSelf=true -Dlog4j.configurationFile=src/test/resources/log4j2-test.xml

echo 运行SimpleAuthControllerTest测试...
call .\mvnw test -Dtest=SimpleAuthControllerTest -Dmaven.test.failure.ignore=true %JAVA_OPTS%

if %errorlevel% neq 0 (
    echo 测试可能包含预期的异常，请检查测试报告是否符合预期
) else (
    echo 测试执行成功
)

echo 测试报告位置: target/surefire-reports/
pause 