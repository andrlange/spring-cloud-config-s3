#!/bin/bash

# Spring Config Server Demo Startup Script
# This script starts all components of the demo in the correct order

set -e

echo "🚀 Starting Spring Config Server Demo"
echo "======================================"

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

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

mkdir -p pids
mkdir -p logs

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "docker-compose is not installed. Please install Docker and docker-compose first."
    exit 1
fi

# Check if maven is available
if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed. Please install Maven first."
    exit 1
fi

print_status "Building all Maven projects..."
mvn clean install -DskipTests
print_success "Maven build completed"

print_status "Starting MinIO S3 storage..."
docker-compose up -d
print_success "MinIO started on http://localhost:9000"

print_status "Waiting for MinIO to be ready..."
for i in {1..30}; do
    if curl -f -s http://localhost:9000/minio/health/live > /dev/null 2>&1; then
        print_success "MinIO is ready!"
        CONTAINER_ID=$(docker ps -q --filter "ancestor=minio/minio:latest")
        docker exec "$CONTAINER_ID" mc alias set local http://localhost:9000 minio-admin minio-password
        docker exec "$CONTAINER_ID" mc mb local/config-bucket --ignore-existing
        docker exec "$CONTAINER_ID" mc cp /config-files/application.yml local/config-bucket/
        docker exec "$CONTAINER_ID" mc cp /config-files/demo-service-test.yml local/config-bucket/
        docker exec "$CONTAINER_ID" mc cp /config-files/demo-service-dev.yml local/config-bucket/
        break
    fi
    if [ $i -eq 30 ]; then
        print_error "MinIO failed to start within 30 seconds"
        exit 1
    fi
    sleep 1
done

# Set VCAP_SERVICES environment variable for local testing
export VCAP_SERVICES='{
  "s3": [
    {
      "name": "config-s3-service",
      "label": "s3",
      "plan": "standard",
      "credentials": {
        "endpoint": "http://localhost:9000",
        "access-key": "minio-admin",
        "secret-key": "minio-password",
        "bucket": "config-bucket",
        "region": "us-east-1"
      }
    }
  ]
}'

print_status "Starting Config Server with VCAP_SERVICES..."
cd config-server
VCAP_SERVICES="$VCAP_SERVICES" mvn spring-boot:run > ../logs/config-server.log 2>&1 &
CONFIG_SERVER_PID=$!
cd ..

print_status "Waiting for Config Server to be ready..."
for i in {1..60}; do
    if curl -f -s http://localhost:8888/actuator/health > /dev/null 2>&1; then
        print_success "Config Server is ready!"
        break
    fi
    if [ $i -eq 60 ]; then
        print_error "Config Server failed to start within 60 seconds"
        exit 1
    fi
    sleep 1
done

print_status "Starting Test Environment Client..."
cd client-test
mvn spring-boot:run > ../logs/client-test.log 2>&1 &
TEST_CLIENT_PID=$!
cd ..

print_status "Starting Dev Environment Client..."
cd client-dev
mvn spring-boot:run > ../logs/client-dev.log 2>&1 &
DEV_CLIENT_PID=$!
cd ..

print_status "Waiting for clients to be ready..."
sleep 15

echo ""
print_success "🎉 All services started successfully!"
echo ""
echo "📋 Service URLs:"
echo "=================="
echo "• MinIO Console:     http://localhost:9001 (admin: minio-admin / minio-password)"
echo "• Config Server:     http://localhost:8888 (user: config-user / config-pass)"
echo "• Test Environment:  http://localhost:8080"
echo "• Dev Environment:   http://localhost:8090"
echo ""
echo "📊 Management Endpoints:"
echo "========================="
echo "• Config Server Health: http://localhost:8889/actuator/health"
echo "• Test Client Health:   http://localhost:8081/actuator/health"
echo "• Dev Client Health:    http://localhost:8091/actuator/health"
echo ""
echo "🔍 Configuration APIs:"
echo "======================"
echo "• Test Config API:      http://localhost:8080/api/config"
echo "• Dev Config API:       http://localhost:8090/api/config"
echo ""
echo "🛑 To stop all services, run: ./stop-demo.sh"
echo ""

# Save PIDs for cleanup
echo $CONFIG_SERVER_PID > pids/config-server.pid
echo $TEST_CLIENT_PID > pids/client-test.pid
echo $DEV_CLIENT_PID > pids/client-dev.pid

print_warning "Press Ctrl+C to stop all services"

# Wait for user interrupt
trap 'echo -e "\n${YELLOW}Stopping all services...${NC}"; ./stop-demo.sh; exit 0' INT
wait