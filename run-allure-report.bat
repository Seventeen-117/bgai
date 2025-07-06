@echo off
echo ======================================
echo 运行测试并生成Allure报告
echo ======================================

REM 设置Java选项
set JAVA_OPTS=-Xms256m -Xmx512m

REM 清理旧的报告
echo 清理旧的报告...
if exist "target\allure-results" rmdir /s /q "target\allure-results"
if exist "target\allure-report" rmdir /s /q "target\allure-report"
echo.

REM 运行测试
echo 运行测试...
call mvn clean test -Dtest=ChatGatWayInternalIdempotenceTest -DfailIfNoTests=false
echo.

REM 检查测试是否成功运行
if %errorlevel% neq 0 (
    echo 测试运行失败，错误代码: %errorlevel%
    exit /b %errorlevel%
)

REM 生成Allure报告
echo 生成Allure报告...
call mvn allure:report
echo.

REM 打开报告
echo 启动Allure报告服务器...
call mvn allure:serve
echo.

echo ======================================
echo Allure报告生成完成
echo ====================================== 