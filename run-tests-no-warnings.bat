@echo off
echo 执行测试（禁止动态代理警告）

REM 设置Java环境和参数
set JAVA_HOME=%JAVA_17_HOME%
set PATH=%JAVA_HOME%\bin;%PATH%
set MAVEN_OPTS=-Djdk.attach.allowAttachSelf=true

echo 当前Java版本:
java -version

echo 运行指定测试类...
set /p test_class=请输入要执行的测试类名称: 

call .\mvnw test -Dtest=%test_class% -Dmaven.test.failure.ignore=true -DfailIfNoTests=false

if %errorlevel% neq 0 (
    echo 测试执行完成，可能存在失败的测试。请查看测试报告。
) else (
    echo 测试执行成功
)

echo 测试报告位置: target/surefire-reports/
pause 