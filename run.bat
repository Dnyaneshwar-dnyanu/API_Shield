@echo off
setlocal
echo ========================================================================
echo Launching APIShield Platform...
echo ========================================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1" %*
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Startup encountered an issue. See logs directory for details.
)
echo.
pause
