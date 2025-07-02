@echo off
echo Starting application with LOCAL profile (no Nacos dependency)...
set JAVA_OPTS=-Xms256m -Xmx512m -XX:+HeapDumpOnOutOfMemoryError
set SPRING_PROFILES_ACTIVE=local
set NACOS_CONFIG_ENABLED=false
set NACOS_DISCOVERY_ENABLED=false

java %JAVA_OPTS% -jar target/bgai-1.0.0-SNAPSHOT.jar 