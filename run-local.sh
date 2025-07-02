#!/bin/bash
echo "Starting application with LOCAL profile (no Nacos dependency)..."
export JAVA_OPTS="-Xms256m -Xmx512m -XX:+HeapDumpOnOutOfMemoryError"
export SPRING_PROFILES_ACTIVE=local
export NACOS_CONFIG_ENABLED=false
export NACOS_DISCOVERY_ENABLED=false

java $JAVA_OPTS -jar target/bgai-1.0.0-SNAPSHOT.jar 