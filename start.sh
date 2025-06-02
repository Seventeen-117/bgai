#!/bin/bash

# JVM 参数配置
JAVA_OPTS="-server \
    -Xms512m \
    -Xmx1024m \
    -Xmn256m \
    -XX:MetaspaceSize=128m \
    -XX:MaxMetaspaceSize=256m \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=./logs/heapdump.hprof \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200"

# 应用名称
APP_NAME="bgai"
JAR_NAME="bgai.jar"

# 输出内存配置信息
echo "Starting $APP_NAME with memory settings:"
echo "Initial Heap Size (-Xms): 512MB"
echo "Maximum Heap Size (-Xmx): 1024MB"
echo "Young Generation Size (-Xmn): 256MB"
echo "Initial Metaspace Size: 128MB"
echo "Maximum Metaspace Size: 256MB"

# 启动应用
java $JAVA_OPTS -jar $JAR_NAME 