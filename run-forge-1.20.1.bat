@echo off
setlocal
if defined JAVA17_HOME set "JAVA_HOME=%JAVA17_HOME%"
cd /d "%~dp0forge-1.20.1"
call gradlew.bat runClient
