@echo off
setlocal

rem 设置Java环境变量
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

rem 输出Java版本
echo Using Java:
java -version
echo.

rem 使用Maven运行应用
echo Starting application with Maven...
call mvnw spring-boot:run -Dspring-boot.run.profiles=dev

endlocal 