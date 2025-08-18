#!/bin/bash

echo "🚀 Deadlock Stats Tracker Server Setup"
echo "======================================"

# Check if running on Windows Subsystem for Linux
if grep -qi microsoft /proc/version; then
    echo "WSL detected. Setting up for Windows environment..."
    
    # Check if Docker Desktop is available
    if command -v docker.exe &> /dev/null; then
        echo "Using Docker Desktop..."
        docker.exe build -t deadlock-app .
        docker.exe run -d -p 8080:8080 --name deadlock-server deadlock-app
        echo "✅ Server started at http://localhost:8080"
        echo "To stop: docker.exe stop deadlock-server"
        echo "To restart: docker.exe start deadlock-server"
    else
        echo "❌ Docker Desktop not found. Please install Docker Desktop for Windows."
        echo "Download from: https://www.docker.com/products/docker-desktop"
    fi
else
    # Regular Linux environment
    echo "Linux environment detected..."
    
    if command -v docker &> /dev/null; then
        echo "Using Docker..."
        docker build -t deadlock-app .
        docker run -d -p 8080:8080 --name deadlock-server deadlock-app
        echo "✅ Server started at http://localhost:8080"
    else
        echo "❌ Docker not found. Please install Docker first."
    fi
fi

echo ""
echo "📋 Manual Setup Instructions:"
echo "1. Install Java 11+ and Maven"
echo "2. Run: mvn clean install"
echo "3. Run: mvn jetty:run"
echo "4. Open: http://localhost:8080"