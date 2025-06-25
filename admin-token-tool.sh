#!/bin/bash

echo "===================================="
echo "BGAI管理员Token获取工具"
echo "===================================="
echo

# 检查是否安装了curl
if ! command -v curl &> /dev/null; then
    echo "错误: 未找到curl工具, 请先安装curl"
    exit 1
fi

# 检查是否在开发环境
ENV=${1:-dev}

# 设置API端点
API_HOST="http://localhost:8688"

echo "尝试获取管理员token..."

# 尝试从开发环境API获取
if [ "$ENV" == "dev" ]; then
    echo "使用开发环境API获取管理员token..."
    curl -s $API_HOST/api/dev-admin-token > admin_token.json
    
    echo
    echo "管理员token信息已保存到admin_token.json"
    echo
    echo "可以使用以下命令测试刷新指定用户的token:"
    echo "curl -X POST -H \"Authorization: Bearer 管理员token\" \"$API_HOST/api/auth/refresh-by-userid?userId=要刷新的用户ID\""
    echo
    cat admin_token.json
    exit 0
fi

# 如果指定了生产环境
if [ "$ENV" == "prod" ]; then
    echo "生产环境下需要通过数据库执行以下SQL来获取管理员token:"
    echo
    echo "SELECT user_id, access_token, token_expire_time FROM t_user WHERE user_id = 'admin-system';"
    echo
    echo "或者先执行初始化SQL:"
    echo "UPDATE t_user SET access_token = UUID(), refresh_token = UUID(), token_expire_time = DATE_ADD(NOW(), INTERVAL 30 DAY) WHERE user_id = 'admin-system';"
    echo
fi

echo "完成!" 