@echo off
setlocal
echo ========================================================================
echo Stopping APIShield Platform...
echo ========================================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop.ps1" %*
echo.
pause
