@echo off
setlocal
set "REPO_ROOT=%~dp0.."
cd /d "%REPO_ROOT%"

if defined JAVA21_HOME set "JAVA_HOME=%JAVA21_HOME%"

set "CHOICE=%~1"
if not "%CHOICE%"=="" goto :HANDLE_CHOICE

:MENU
cls
echo ====================================================
echo       Camera Lock-On - NeoForge Dev Client
echo ====================================================
echo   1) NeoForge 1.21.1
echo   0) Exit
echo ----------------------------------------------------
set /p "CHOICE=Select version to run [1, default=1]: "
if "%CHOICE%"=="" set "CHOICE=1"

:HANDLE_CHOICE
if "%CHOICE%"=="1" goto :RUN_1211
if "%CHOICE%"=="1.21.1" goto :RUN_1211
if "%CHOICE%"=="1211" goto :RUN_1211

if "%CHOICE%"=="0" exit /b 0

echo Invalid selection: "%CHOICE%"
timeout /t 2 >nul
goto :MENU

:RUN_1211
set "TARGET_DIR=neoforge-1.21.1"
goto :START_CLIENT

:START_CLIENT
echo [Camera Lock-On] Running %TARGET_DIR% client...
cd /d "%REPO_ROOT%\%TARGET_DIR%"
call gradlew.bat runClient
set "EXITCODE=%ERRORLEVEL%"
cd /d "%REPO_ROOT%"
exit /b %EXITCODE%
