#!/bin/bash

# Spring Config Server Demo Shutdown Script
# This script stops all components of the demo

set -e

echo "🛑 Stopping Spring Config Server Demo"
echo "====================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Stop Spring Boot applications
if [ -f pids/config-server.pid ]; then
    CONFIG_SERVER_PID=$(cat pids/config-server.pid)
    if kill -0 $CONFIG_SERVER_PID 2>/dev/null; then
        print_status "Stopping Config Server (PID: $CONFIG_SERVER_PID)..."
        kill $CONFIG_SERVER_PID
        print_success "Config Server stopped"
    fi
    rm -f pids/config-server.pid
fi

if [ -f pids/client-test.pid ]; then
    TEST_CLIENT_PID=$(cat pids/client-test.pid)
    if kill -0 $TEST_CLIENT_PID 2>/dev/null; then
        print_status "Stopping Test Client (PID: $TEST_CLIENT_PID)..."
        kill $TEST_CLIENT_PID
        print_success "Test Client stopped"
    fi
    rm -f pids/client-test.pid
fi

if [ -f pids/client-dev.pid ]; then
    DEV_CLIENT_PID=$(cat pids/client-dev.pid)
    if kill -0 $DEV_CLIENT_PID 2>/dev/null; then
        print_status "Stopping Dev Client (PID: $DEV_CLIENT_PID)..."
        kill $DEV_CLIENT_PID
        print_success "Dev Client stopped"
    fi
    rm -f pids/client-dev.pid
fi

# Stop any remaining Spring Boot processes
print_status "Stopping any remaining Spring Boot processes..."
pkill -f "spring-boot:run" || true

# Stop Docker containers
print_status "Stopping Docker containers..."
docker-compose down
print_success "Docker containers stopped"

# Clean up
print_status "Cleaning up..."
rm -rf pids/
rm -rf logs/

print_success "🎉 All services stopped successfully!"