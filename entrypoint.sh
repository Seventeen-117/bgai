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

# 启动应用
echo "Starting application with profile: $SPRING_PROFILES_ACTIVE"
exec java $JAVA_OPTS \
    -Drocketmq.client.logRoot=/app/logs \
    -Drocketmq.log.dir=/app/logs \
    -Dseata.saga.state-machine.auto-register=$SEATA_SAGA_STATE_MACHINE_AUTO_REGISTER \
    -jar app.jar 