@echo off
setlocal

REM 测试动态路由API的脚本

REM 服务器地址
set SERVER=http://localhost:8688

echo ===== 动态路由测试脚本 =====

REM 1. 获取所有路由
echo.
echo 1. 获取所有路由
curl -X GET %SERVER%/gateway/routes

REM 2. 添加单个路由
echo.
echo 2. 添加单个路由
curl -X POST %SERVER%/gateway/routes ^
  -H "Content-Type: application/json" ^
  -d @test-dynamic-route.json

REM 3. 再次获取所有路由，查看是否添加成功
echo.
echo 3. 验证路由是否添加成功
curl -X GET %SERVER%/gateway/routes

REM 4. 获取特定路由
echo.
echo 4. 获取特定路由
curl -X GET %SERVER%/gateway/routes/api-test-route

REM 5. 批量添加路由
echo.
echo 5. 批量添加路由
curl -X POST %SERVER%/gateway/routes/batch ^
  -H "Content-Type: application/json" ^
  -d @test-dynamic-routes-batch.json

REM 6. 再次获取所有路由，查看是否批量添加成功
echo.
echo 6. 验证批量添加是否成功
curl -X GET %SERVER%/gateway/routes

REM 7. 更新路由
echo.
echo 7. 更新路由
curl -X PUT %SERVER%/gateway/routes ^
  -H "Content-Type: application/json" ^
  -d "{\"id\":\"api-test-route\",\"predicates\":[{\"name\":\"Path\",\"args\":{\"pattern\":\"/api/test-updated/**\"}}],\"filters\":[{\"name\":\"StripPrefix\",\"args\":{\"parts\":\"1\"}},{\"name\":\"AddResponseHeader\",\"args\":{\"name\":\"X-Response-From\",\"value\":\"API-Gateway-Updated\"}}],\"uri\":\"http://localhost:8688/test-updated\",\"order\":0}"

REM 8. 获取更新后的路由
echo.
echo 8. 获取更新后的路由
curl -X GET %SERVER%/gateway/routes/api-test-route

REM 9. 刷新路由
echo.
echo 9. 刷新路由
curl -X POST %SERVER%/gateway/routes/refresh

REM 10. 删除路由
echo.
echo 10. 删除路由
curl -X DELETE %SERVER%/gateway/routes/api-test-route

REM 11. 批量删除路由
echo.
echo 11. 批量删除路由
curl -X DELETE %SERVER%/gateway/routes/batch ^
  -H "Content-Type: application/json" ^
  -d "[\"api-user-route\", \"api-product-route\"]"

REM 12. 最终获取所有路由，验证删除是否成功
echo.
echo 12. 验证删除是否成功
curl -X GET %SERVER%/gateway/routes

echo.
echo ===== 测试完成 =====
pause 