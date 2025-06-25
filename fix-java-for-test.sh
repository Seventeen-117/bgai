#!/bin/bash

echo "尝试为测试环境配置Java 17..."

# 检查是否有设置JAVA_HOME_17环境变量
if [ ! -z "$JAVA_HOME_17" ]; then
    echo "使用已有的JAVA_HOME_17: $JAVA_HOME_17"
    export JAVA_HOME=$JAVA_HOME_17
else
    # 尝试常见的Java 17安装路径
    if [ -d "/usr/lib/jvm/java-17-openjdk" ]; then
        echo "使用/usr/lib/jvm/java-17-openjdk作为JAVA_HOME"
        export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
    elif [ -d "/usr/lib/jvm/java-17-oracle" ]; then
        echo "使用/usr/lib/jvm/java-17-oracle作为JAVA_HOME"
        export JAVA_HOME=/usr/lib/jvm/java-17-oracle
    elif [ -d "/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home" ]; then
        echo "使用macOS Java 17"
        export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
    else
        echo "未找到Java 17，请安装Java 17或设置JAVA_HOME_17环境变量"
        echo "设置方法: export JAVA_HOME_17=/path/to/java17"
        exit 1
    fi
fi

# 确认Java版本
echo "当前JAVA_HOME: $JAVA_HOME"
"$JAVA_HOME/bin/java" -version

# 临时设置PATH
export PATH=$JAVA_HOME/bin:$PATH

# 执行Maven测试
echo "运行测试..."
./mvnw test-compile
./mvnw test -Dtest=SimpleChatGatewayInternalTest

# 提示按任意键继续
read -p "按任意键继续..." key 