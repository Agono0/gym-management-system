@echo off
title Gym-OS Terminal Workflow Console
call "%~dp0gradlew.bat" runCli -q --console=plain %*
if errorlevel 1 (
    echo.
    echo [ERROR] Gym-OS CLI exited with code %errorlevel%.
    pause
)

