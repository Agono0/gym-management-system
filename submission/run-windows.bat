@echo off
setlocal
title GYM Management System
cd /d "%~dp0"

if not exist "out" mkdir "out"

rem Relative paths per package, so a folder name with spaces still compiles (add new packages here)
javac --release 21 -encoding UTF-8 -Xlint:all -cp "lib\*" -d "out" src\com\gym\*.java src\com\gym\dao\*.java src\com\gym\exception\*.java src\com\gym\model\*.java src\com\gym\util\*.java
if errorlevel 1 (
    echo.
    echo [ERROR] Compilation failed. Verify JDK 21 or newer is installed on PATH.
    pause
    exit /b 1
)

java --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 -cp "out;lib\*" com.gym.Main
if errorlevel 1 (
    echo.
    echo [ERROR] GYM Management System exited with code %errorlevel%.
    pause
    exit /b %errorlevel%
)

