@echo off
setlocal EnableExtensions
set "GRADLE_VERSION=8.10.2"
set "GRADLE_DIST=gradle-%GRADLE_VERSION%-bin.zip"
set "GRADLE_URL=https://services.gradle.org/distributions/%GRADLE_DIST%"
set "GRADLE_CACHE=%USERPROFILE%\.gradle\camera-lockon-distributions"
set "GRADLE_HOME=%GRADLE_CACHE%\gradle-%GRADLE_VERSION%"
set "GRADLE_BAT=%GRADLE_HOME%\bin\gradle.bat"

if not exist "%GRADLE_BAT%" (
    echo Gradle %GRADLE_VERSION% is not installed in the local cache.
    echo Downloading %GRADLE_URL% ...
    if not exist "%GRADLE_CACHE%" mkdir "%GRADLE_CACHE%"
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $zip=Join-Path $env:TEMP '%GRADLE_DIST%'; Invoke-WebRequest -UseBasicParsing '%GRADLE_URL%' -OutFile $zip; if (Test-Path '%GRADLE_HOME%') { Remove-Item -Recurse -Force '%GRADLE_HOME%' }; Expand-Archive -Path $zip -DestinationPath '%GRADLE_CACHE%' -Force; Remove-Item $zip -Force"
    if errorlevel 1 (
        echo Failed to download or extract Gradle %GRADLE_VERSION%.
        exit /b 1
    )
)

call "%GRADLE_BAT%" %*
exit /b %ERRORLEVEL%
