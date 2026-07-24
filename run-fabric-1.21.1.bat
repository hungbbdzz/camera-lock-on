@echo off
setlocal
if defined JAVA21_HOME set "JAVA_HOME=%JAVA21_HOME%"
cd /d "%~dp0fabric-1.21.1"
call gradlew.bat runClient
