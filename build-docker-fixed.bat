@echo off
echo ==== BGAI Docker 构建 (含 Nacos 修复) ====
echo 构建修复了 Nacos 配置目录问题的 Docker 镜像
echo.

rem 清理 Docker 缓存
echo 清理 Docker 构建缓存...
docker builder prune -f

rem 构建 Docker 镜像
echo 开始构建 Docker 镜像...
docker build --network=host -f Dockerfile.fixed -t jiangyang-ai:latest .

echo.
echo 构建完成！现在可以使用以下命令运行容器：
echo   docker run -p 8080:8080 jiangyang-ai:latest
echo.
