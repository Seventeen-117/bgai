#!/bin/bash

# 测试动态路由API的脚本

# 服务器地址
SERVER="http://localhost:8688"

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}===== 动态路由测试脚本 =====${NC}"

# 1. 获取所有路由
echo -e "${YELLOW}\n1. 获取所有路由${NC}"
curl -s -X GET ${SERVER}/gateway/routes | jq

# 2. 添加单个路由
echo -e "${YELLOW}\n2. 添加单个路由${NC}"
curl -s -X POST ${SERVER}/gateway/routes \
  -H "Content-Type: application/json" \
  -d @test-dynamic-route.json

# 3. 再次获取所有路由，查看是否添加成功
echo -e "${YELLOW}\n3. 验证路由是否添加成功${NC}"
curl -s -X GET ${SERVER}/gateway/routes | jq

# 4. 获取特定路由
echo -e "${YELLOW}\n4. 获取特定路由${NC}"
curl -s -X GET ${SERVER}/gateway/routes/api-test-route | jq

# 5. 批量添加路由
echo -e "${YELLOW}\n5. 批量添加路由${NC}"
curl -s -X POST ${SERVER}/gateway/routes/batch \
  -H "Content-Type: application/json" \
  -d @test-dynamic-routes-batch.json

# 6. 再次获取所有路由，查看是否批量添加成功
echo -e "${YELLOW}\n6. 验证批量添加是否成功${NC}"
curl -s -X GET ${SERVER}/gateway/routes | jq

# 7. 更新路由
echo -e "${YELLOW}\n7. 更新路由${NC}"
curl -s -X PUT ${SERVER}/gateway/routes \
  -H "Content-Type: application/json" \
  -d '{
    "id": "api-test-route",
    "predicates": [
      {
        "name": "Path",
        "args": {
          "pattern": "/api/test-updated/**"
        }
      }
    ],
    "filters": [
      {
        "name": "StripPrefix",
        "args": {
          "parts": "1"
        }
      },
      {
        "name": "AddResponseHeader",
        "args": {
          "name": "X-Response-From",
          "value": "API-Gateway-Updated"
        }
      }
    ],
    "uri": "http://localhost:8688/test-updated",
    "order": 0
  }'

# 8. 获取更新后的路由
echo -e "${YELLOW}\n8. 获取更新后的路由${NC}"
curl -s -X GET ${SERVER}/gateway/routes/api-test-route | jq

# 9. 刷新路由
echo -e "${YELLOW}\n9. 刷新路由${NC}"
curl -s -X POST ${SERVER}/gateway/routes/refresh

# 10. 删除路由
echo -e "${YELLOW}\n10. 删除路由${NC}"
curl -s -X DELETE ${SERVER}/gateway/routes/api-test-route

# 11. 批量删除路由
echo -e "${YELLOW}\n11. 批量删除路由${NC}"
curl -s -X DELETE ${SERVER}/gateway/routes/batch \
  -H "Content-Type: application/json" \
  -d '["api-user-route", "api-product-route"]'

# 12. 最终获取所有路由，验证删除是否成功
echo -e "${YELLOW}\n12. 验证删除是否成功${NC}"
curl -s -X GET ${SERVER}/gateway/routes | jq

echo -e "${GREEN}\n===== 测试完成 =====${NC}" 