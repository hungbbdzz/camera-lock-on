@echo off
setlocal
cd /d "%~dp0"

if defined JAVA21_HOME set "JAVA_HOME=%JAVA21_HOME%"

set "CHOICE=%~1"
if not "%CHOICE%"=="" goto :HANDLE_CHOICE

:MENU
cls
echo ====================================================
echo       Camera Lock-On - NeoForge Dev Client
echo ====================================================
echo   1) NeoForge 1.21.11
echo   2) NeoForge 1.21.4
echo   3) NeoForge 1.21.1
echo   0) Exit
echo ----------------------------------------------------
set /p "CHOICE=Select version to run [1-3]: "

:HANDLE_CHOICE
if "%CHOICE%"=="1" goto :RUN_12111
if "%CHOICE%"=="1.21.11" goto :RUN_12111
if "%CHOICE%"=="12111" goto :RUN_12111

if "%CHOICE%"=="2" goto :RUN_1214
if "%CHOICE%"=="1.21.4" goto :RUN_1214
if "%CHOICE%"=="1214" goto :RUN_1214

if "%CHOICE%"=="3" goto :RUN_1211
if "%CHOICE%"=="1.21.1" goto :RUN_1211
if "%CHOICE%"=="1211" goto :RUN_1211

if "%CHOICE%"=="0" exit /b 0

echo Invalid selection: "%CHOICE%"
timeout /t 2 >nul
goto :MENU

:RUN_12111
set "TARGET_DIR=neoforge-1.21.11"
goto :START_CLIENT

:RUN_1214
set "TARGET_DIR=neoforge-1.21.4"
goto :START_CLIENT

:RUN_1211
set "TARGET_DIR=neoforge-1.21.1"
goto :START_CLIENT

:START_CLIENT
echo [Camera Lock-On] Running %TARGET_DIR% client...
cd /d "%~dp0%TARGET_DIR%"
call gradlew.bat runClient
set "EXITCODE=%ERRORLEVEL%"
cd /d "%~dp0"
if not "%EXITCODE%"=="0" (
    echo.
    echo [Camera Lock-On] Client exited with error code %EXITCODE%.
    pause
)
exit /b %EXITCODE%
