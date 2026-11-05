@echo off
setlocal
cd /d "%~dp0"

set "CHOICE=%~1"
if not "%CHOICE%"=="" goto :HANDLE_CHOICE

:MENU
cls
echo =====================================================================
echo               CAMERA LOCK-ON - CLIENT LAUNCHER
echo =====================================================================
echo   1) NeoForge 1.21.1   (Java 21)
echo   2) NeoForge 1.21.11  (Java 21)
echo   3) Fabric 1.21.1     (Java 21)
echo   4) Fabric 1.21.11    (Java 21)
echo   5) Forge 1.20.1      (Java 17)
echo.
echo   0) Exit
echo ---------------------------------------------------------------------
set /p "CHOICE=Select a loader/version [1-5]: "

:HANDLE_CHOICE
if "%CHOICE%"=="1" (
    call "%~dp0scripts\run-neoforge.bat" 1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="2" (
    call "%~dp0scripts\run-neoforge.bat" 2
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="3" (
    call "%~dp0scripts\run-fabric.bat" 1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="4" (
    call "%~dp0scripts\run-fabric.bat" 2
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="5" (
    call "%~dp0scripts\run-forge-1.20.1.bat"
    exit /b %ERRORLEVEL%
)

if "%CHOICE%"=="0" exit /b 0

echo Invalid selection: "%CHOICE%"
timeout /t 2 >nul
goto :MENU

