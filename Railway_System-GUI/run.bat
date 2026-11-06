@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out src\*.java
if errorlevel 1 (
  echo Java compilation failed. Check that the JDK is installed.
  pause
  exit /b 1
)
java -cp out Main
if errorlevel 1 pause
