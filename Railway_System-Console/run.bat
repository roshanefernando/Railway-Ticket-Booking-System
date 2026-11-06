@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -d out src\*.java
if errorlevel 1 (
    echo Compilation failed. Make sure Java JDK is installed and javac is in PATH.
    pause
    exit /b 1
)
java -cp out Main
pause
