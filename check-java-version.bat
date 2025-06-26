@echo off
echo Checking Java installations...

REM Try multiple potential Java homes
set JAVA_PATHS=^
C:\Program Files\Java\jdk-17\bin\java.exe;^
C:\Program Files\Java\jdk-17.0.1\bin\java.exe;^
C:\Program Files\Java\jdk-17.0.2\bin\java.exe;^
C:\Program Files\Java\jdk-17.0.3\bin\java.exe;^
C:\Program Files\Java\jdk-17.0.4\bin\java.exe;^
C:\Program Files\Java\jdk-17.0.5\bin\java.exe

echo Default Java version:
java -version
echo.

for %%j in (%JAVA_PATHS%) do (
  if exist "%%j" (
    echo Found Java at: %%j
    "%%j" -version
    echo.
  )
)

echo If you see Java 17 above, use that path to run your application. 
pause 