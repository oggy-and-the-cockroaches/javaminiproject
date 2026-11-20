@echo off
title DiskPilot Launcher
set "JAVA_EXE=C:\Program Files\Java\jdk-27.0.1.0.3\bin\javaw.exe"
if not exist "%JAVA_EXE%" (
    set "JAVA_EXE=javaw"
)
start "" "%JAVA_EXE%" -jar "%~dp0diskpilot-1.0.0.jar"
exit
