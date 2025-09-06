#!/bin/bash

# Spring Config Server Demo Test Script
# This script tests all endpoints and verifies the demo is working correctly

set -e

echo "🧪 Testing Spring Config Server Demo"
echo "==================================="


# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_test() {
    echo -e "${BLUE}[TEST]${NC} $1"
}

print_pass() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

print_fail() {
    echo -e "${RED}[FAIL]${NC} $1"
}

# Test counter
TESTS=0
PASSED=0

run_test() {
    local name="$1"
    local command="$2"
    local expected="$3"
    
    TESTS=$((TESTS + 1))
    print_test "$name"
    
    if eval "$command" &>/dev/null; then
        if [ -n "$expected" ]; then
            result=$(eval "$command" 2>/dev/null | grep -q "$expected" && echo "found" || echo "not_found")
            if [ "$result" = "found" ]; then
                print_pass "$name"
                PASSED=$((PASSED + 1))
            else
                print_fail "$name - Expected content not found"
            fi
        else
            print_pass "$name"
            PASSED=$((PASSED + 1))
        fi
    else
        print_fail "$name"
    fi
}

echo ""
echo "🔍 Testing Service Health..."

# Test MinIO
run_test "MinIO Health Check" "curl -f -s http://localhost:9000/minio/health/live"

# Test Config Server
run_test "Config Server Health" "curl -f -s http://localhost:8889/actuator/health" '"status":"UP"'

# Test Test Client
run_test "Test Client Health" "curl -f -s http://localhost:8081/actuator/health" '"status":"UP"'

# Test Dev Client  
run_test "Dev Client Health" "curl -f -s http://localhost:8091/actuator/health" '"status":"UP"'

echo ""
echo "📋 Testing Configuration APIs..."

# Test configuration endpoints
run_test "Test Client Config API" "curl -f -s http://localhost:8080/api/config" '"environment":"TEST"'
run_test "Dev Client Config API" "curl -f -s http://localhost:8090/api/config" '"environment":"DEV"'

# Test Config Server direct access
run_test "Config Server Test Profile" "curl -f -s -u config-user:config-pass http://localhost:8888/demo-service/test" 'test-value-alpha'
run_test "Config Server Dev Profile" "curl -f -s -u config-user:config-pass http://localhost:8888/demo-service/dev" 'dev-value-apple'

echo ""
echo "🌐 Testing Web Interfaces..."

# Test web pages
run_test "Test Client Web Page" "curl -f -s http://localhost:8080" 'Spring Config Demo'
run_test "Dev Client Web Page" "curl -f -s http://localhost:8090" 'Spring Config Demo'

echo ""
echo "📊 Test Results"
echo "==============="
echo "Tests Run: $TESTS"
echo "Passed: $PASSED"
echo "Failed: $((TESTS - PASSED))"

if [ $PASSED -eq $TESTS ]; then
    echo -e "${GREEN}🎉 All tests passed! Demo is working correctly.${NC}"
    exit 0
else
    echo -e "${RED}❌ Some tests failed. Please check the services.${NC}"
    exit 1
fi