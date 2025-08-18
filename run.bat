@echo off
echo 🚀 Deadlock Stats Tracker Server
echo ===============================

REM Check if Maven is installed
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ Maven not found. Please install Maven first.
    echo Download from: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

REM Check if Java is installed
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ Java not found. Please install Java 11+ first.
    echo Download from: https://adoptium.net/
    pause
    exit /b 1
)

echo ✅ Java and Maven found
echo 📦 Building project...
call mvn clean install

if %errorlevel% neq 0 (
    echo ❌ Build failed
    pause
    exit /b 1
)

echo 🚀 Starting server...
echo Server will be available at: http://localhost:8080
echo Press Ctrl+C to stop the server
call mvn jetty:run

pause