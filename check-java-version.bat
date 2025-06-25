@echo off
echo Checking Java versions...

echo.
echo Current Java version:
java -version
echo.

echo Java Home:
echo %JAVA_HOME%
echo.

echo Java Home 17:
echo %JAVA_HOME_17%
echo.

echo Looking for Java 17 installations...
where java | findstr "17"
echo.

echo System Path:
echo %PATH%
echo.

echo Done.
pause 