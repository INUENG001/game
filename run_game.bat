@echo off
setlocal
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out src\main\java\th\roadmillionaire\*.java
if errorlevel 1 exit /b 1
start "" javaw -cp out th.roadmillionaire.Main
