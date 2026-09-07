@echo off
setlocal
if defined JAVA21_HOME set "JAVA_HOME=%JAVA21_HOME%"
cd /d "%~dp0neoforge-1.21.11"
call gradlew.bat runClient
