#!/bin/bash
echo "======================================"
echo "运行测试并生成Allure报告"
echo "======================================"

# 设置Java选项
export JAVA_OPTS="-Xms256m -Xmx512m"

# 清理旧的报告
echo "清理旧的报告..."
rm -rf target/allure-results
rm -rf target/allure-report
echo ""

# 运行测试
echo "运行测试..."
mvn clean test -Dtest=ChatGatWayInternalIdempotenceTest -DfailIfNoTests=false
echo ""

# 检查测试是否成功运行
if [ $? -ne 0 ]; then
    echo "测试运行失败，错误代码: $?"
    exit $?
fi

# 生成Allure报告
echo "生成Allure报告..."
mvn allure:report
echo ""

# 打开报告
echo "启动Allure报告服务器..."
mvn allure:serve
echo ""

echo "======================================"
echo "Allure报告生成完成"
echo "======================================" 