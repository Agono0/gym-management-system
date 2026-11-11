@echo off
setlocal
title GYM Management System
cd /d "%~dp0"

if not exist "out" mkdir "out"
dir /s /b "src\*.java" > "out\sources.txt"

javac --release 21 -encoding UTF-8 -Xlint:all -cp "lib\*" -d "out" @"out\sources.txt"
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

