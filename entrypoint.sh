#!/bin/bash

# 设置Seata相关配置
echo "Setting up Seata configuration..."

# 确保auto-register配置生效
if [[ -f /app/file.conf ]]; then
    echo "Modifying file.conf to disable state machine auto-registration"
    sed -i 's/auto-register = true/auto-register = false/g' /app/file.conf
    echo "Updated file.conf"
fi

# 可选：完全禁用Seata
if [[ "$DISABLE_SEATA" == "true" ]]; then
    echo "Disabling Seata completely"
    export SEATA_ENABLED=false
    export SAGA_ENABLED=false
fi

# 显示当前配置
echo "Current Seata configuration:"
echo " - SEATA_SAGA_STATE_MACHINE_AUTO_REGISTER: $SEATA_SAGA_STATE_MACHINE_AUTO_REGISTER"
echo " - SEATA_ENABLED: $SEATA_ENABLED"
echo " - SAGA_ENABLED: $SAGA_ENABLED"

# 检查JAR文件是否存在
if [[ ! -f /app/app.jar ]]; then
    echo "ERROR: JAR file not found at /app/app.jar"
    echo "Checking available jar files:"
    find /app -name "*.jar" -type f | xargs ls -la
    exit 1
fi

# 验证JAR文件
echo "Verifying JAR file manifest:"
jar -tvf /app/app.jar | grep -i 'Main-Class' || echo "WARNING: No Main-Class found in JAR manifest"

# 启动应用
echo "Starting application with profile: $SPRING_PROFILES_ACTIVE"
exec java $JAVA_OPTS \
    -Drocketmq.client.logRoot=/app/logs \
    -Drocketmq.log.dir=/app/logs \
    -Dseata.saga.state-machine.auto-register=$SEATA_SAGA_STATE_MACHINE_AUTO_REGISTER \
    -Dmanagement.simple.metrics.export.enabled=false \
    -Dmanagement.metrics.enable.all=false \
    -jar /app/app.jar 