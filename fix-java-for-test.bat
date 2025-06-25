@echo off
echo Attempting to configure Java 17 for tests...

:: Check for JAVA_HOME_17 environment variable
if defined JAVA_HOME_17 (
    echo Using existing JAVA_HOME_17: %JAVA_HOME_17%
    set JAVA_HOME=%JAVA_HOME_17%
) else (
    :: Try common Java 17 installation paths
    if exist "C:\Program Files\Java\jdk-17" (
        echo Found JDK 17 at C:\Program Files\Java\jdk-17
        set JAVA_HOME=C:\Program Files\Java\jdk-17
    ) else if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.8.101-hotspot" (
        echo Found Eclipse Adoptium JDK 17
        set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.8.101-hotspot
    ) else if exist "C:\Program Files\Java\jdk-17.0.1" (
        echo Found JDK 17.0.1 at C:\Program Files\Java\jdk-17.0.1
        set JAVA_HOME=C:\Program Files\Java\jdk-17.0.1
    ) else if exist "C:\Program Files\Eclipse Adoptium\jdk-17" (
        echo Found Eclipse Adoptium JDK 17
        set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17
    ) else (
        echo ERROR: Java 17 not found!
        echo.
        echo You need to install Java 17 and set the JAVA_HOME_17 environment variable.
        echo.
        echo Installation Options:
        echo 1. Download OpenJDK 17 from: https://adoptium.net/
        echo 2. Or download Oracle JDK 17 from: https://www.oracle.com/java/technologies/downloads/#java17
        echo.
        echo After installation, set the JAVA_HOME_17 environment variable:
        echo 1. Right-click on My Computer -^> Properties -^> Advanced System Settings -^> Environment Variables
        echo 2. Add a new variable named JAVA_HOME_17 with the value of your Java 17 installation path
        echo.
        pause
        exit /b 1
    )
)

:: Verify Java version
echo Current JAVA_HOME: %JAVA_HOME%
"%JAVA_HOME%\bin\java" -version 2>&1 | findstr "version"

:: Check if we actually have Java 17
"%JAVA_HOME%\bin\java" -version 2>&1 | findstr "17" > nul
if errorlevel 1 (
    echo ERROR: The configured Java is not version 17!
    echo Current version details:
    "%JAVA_HOME%\bin\java" -version
    echo.
    echo Please install Java 17 and set JAVA_HOME_17 correctly.
    pause
    exit /b 1
)

:: Set up environment for Maven test
set PATH=%JAVA_HOME%\bin;%PATH%
set MAVEN_OPTS=-Xmx512m -Duser.language=en
set _JAVA_OPTIONS=-Duser.language=en
set JAVA_TOOL_OPTIONS=-Duser.language=en
set JAVA_VERSION=17
set MAVEN_SKIP_RC=true

echo.
echo Java 17 configured successfully. Now running tests...
echo.

:: Run tests
echo Compiling test classes...
call mvnw clean test-compile -Dmaven.main.skip=true

echo.
echo Running simple unit test (no Spring context)...
call mvnw test -Dtest=SimpleUnitTest

echo.
echo Running SimpleChatControllerMockTest (Mockito only, no Spring context)...
call mvnw test -Dtest=SimpleChatControllerMockTest

echo.
echo If these tests pass, you can try more complex tests:
echo call mvnw test -Dtest=ReactiveChatControllerTest
echo.

pause 