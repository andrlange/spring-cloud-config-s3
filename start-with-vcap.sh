#!/bin/bash

# Start Config Server with VCAP_SERVICES from JSON file
# This script demonstrates how to run the application with VCAP services locally

set -e

echo "🔧 Starting Spring Config Server with VCAP_SERVICES"
echo "=================================================="

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

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

# Check if vcap-local.json exists
if [[ ! -f "vcap-local.json" ]]; then
    print_error "vcap-local.json file not found!"
    print_status "Creating sample vcap-local.json file..."
    cat > vcap-local.json << 'EOF'
{
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
}
EOF
    print_success "Created vcap-local.json with default MinIO configuration"
fi

# Extract VCAP_SERVICES from JSON file (excluding comments)
VCAP_SERVICES=$(cat vcap-local.json | jq 'del(.["comment"], .["comment-production"], .["comment-aws"], .["dell-ecs-example"], .["aws-s3-example"])')

if [[ -z "$VCAP_SERVICES" ]]; then
    print_error "Failed to parse VCAP_SERVICES from vcap-local.json"
    exit 1
fi

print_status "Using VCAP_SERVICES configuration:"
echo "$VCAP_SERVICES" | jq .

# Check if MinIO is running
if ! curl -f -s http://localhost:9000/minio/health/live > /dev/null 2>&1; then
    print_warning "MinIO not detected at localhost:9000"
    print_status "Starting MinIO with docker-compose..."
    docker-compose up -d
    
    print_status "Waiting for MinIO to be ready..."
    for i in {1..30}; do
        if curl -f -s http://localhost:9000/minio/health/live > /dev/null 2>&1; then
            print_success "MinIO is ready!"
            break
        fi
        if [ $i -eq 30 ]; then
            print_error "MinIO failed to start within 30 seconds"
            exit 1
        fi
        sleep 1
    done
else
    print_success "MinIO is already running"
fi

# Build the project
print_status "Building config-server..."
cd config-server
mvn clean compile -q
cd ..

# Start the config server with VCAP_SERVICES
print_status "Starting Config Server with VCAP_SERVICES..."
cd config-server

export VCAP_SERVICES="$VCAP_SERVICES"
export SPRING_PROFILES_ACTIVE="cloud"

print_success "Config Server starting with Cloud Foundry profile and VCAP services"
print_status "VCAP_SERVICES environment variable set"
print_status "Check the logs to see VCAP services being parsed"

mvn spring-boot:run

cd ..