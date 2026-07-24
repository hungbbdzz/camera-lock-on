@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-all.ps1"
set "RESULT=%ERRORLEVEL%"
if not "%RESULT%"=="0" (
    echo.
    echo Build failed. Read the error above.
    pause
)
exit /b %RESULT%
