@echo off
echo ====================================
echo BGAI管理员Token获取工具
echo ====================================
echo.

REM 检查是否安装了curl
where curl >nul 2>nul
if %errorlevel% neq 0 (
    echo 错误: 未找到curl工具, 请先安装curl
    exit /b 1
)

REM 检查是否在开发环境
set ENV=%1
if "%ENV%"=="" set ENV=dev

REM 设置API端点
set API_HOST=http://localhost:8688

echo 尝试获取管理员token...

REM 尝试从开发环境API获取
if "%ENV%"=="dev" (
    echo 使用开发环境API获取管理员token...
    curl -s %API_HOST%/api/dev-admin-token > admin_token.json
    
    echo.
    echo 管理员token信息已保存到admin_token.json
    echo.
    echo 可以使用以下命令测试刷新指定用户的token:
    echo curl -X POST -H "Authorization: Bearer 管理员token" "%API_HOST%/api/auth/refresh-by-userid?userId=要刷新的用户ID"
    echo.
    type admin_token.json
    exit /b 0
)

REM 如果指定了生产环境
if "%ENV%"=="prod" (
    echo 生产环境下需要通过数据库执行以下SQL来获取管理员token:
    echo.
    echo "SELECT user_id, access_token, token_expire_time FROM t_user WHERE user_id = 'admin-system';"
    echo.
    echo 或者先执行初始化SQL:
    echo "UPDATE t_user SET access_token = UUID(), refresh_token = UUID(), token_expire_time = DATE_ADD(NOW(), INTERVAL 30 DAY) WHERE user_id = 'admin-system';"
    echo.
)

echo 完成! 