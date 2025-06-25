@echo off
echo ===== Java Version Fix for Tests =====
echo This script will help configure the right Java version for running tests

echo.
echo Checking current Java version...
java -version

echo.
echo Checking available Java installations...

where java
echo.

set /p JAVA_HOME_PATH=Enter the path to your JDK 21 installation (e.g., C:\Program Files\Java\jdk-21): 

if not exist "%JAVA_HOME_PATH%\bin\java.exe" (
    echo Error: Java executable not found at %JAVA_HOME_PATH%\bin\java.exe
    echo Please verify the path and try again.
    exit /b 1
)

echo.
echo Setting JAVA_HOME to %JAVA_HOME_PATH%...
set JAVA_HOME=%JAVA_HOME_PATH%
set PATH=%JAVA_HOME%\bin;%PATH%

echo.
echo Verifying new Java version...
java -version

echo.
echo Cleaning and recompiling the project with the new Java version...
call mvnw clean compile test-compile

echo.
echo Setup complete!
echo You can now run tests with JDK 21. Example:
echo call mvnw test -Dtest=ChatGatWayInternalTest#testChatGatWayInternal_WithXUserIdHeader_Integration
echo.
echo Note: This Java version change is only active in the current terminal session.
echo To make it permanent, set JAVA_HOME in your system environment variables.

pause 