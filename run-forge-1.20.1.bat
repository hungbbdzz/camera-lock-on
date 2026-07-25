@echo off
setlocal
cd /d "%~dp0"

rem Forge 1.20.1 / ForgeGradle must run on JDK 17.
if defined JAVA17_HOME (
    set "JAVA_HOME=%JAVA17_HOME%"
) else if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-17"
) else if exist "C:\Program Files\Eclipse Adoptium\jdk-17\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17"
) else (
    echo [Camera Lock-On] JDK 17 was not found.
    echo Set JAVA17_HOME to the JDK 17 folder and reopen this terminal.
    pause
    exit /b 1
)

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [Camera Lock-On] java.exe was not found at:
    echo %JAVA_HOME%\bin\java.exe
    pause
    exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
echo [Camera Lock-On] Using Java:
"%JAVA_HOME%\bin\java.exe" -version

cd /d "%~dp0forge-1.20.1"
call gradlew.bat --stop >nul 2>&1
call gradlew.bat runClient

if errorlevel 1 (
    echo.
    echo [Camera Lock-On] Forge 1.20.1 failed to start.
    pause
    exit /b 1
)

endlocal
