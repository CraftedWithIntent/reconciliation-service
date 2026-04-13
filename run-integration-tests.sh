#!/bin/bash

#################################################################################
# Integration Test Runner for Oracle → PostgreSQL Reconciliation
# 
# This standalone script runs integration tests outside of Gradle,
# providing direct control over Docker configuration and test execution.
#
# Usage:
#   ./run-integration-tests.sh
#   ./run-integration-tests.sh --debug
#   ./run-integration-tests.sh --clean
#
#################################################################################

set -e

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$SCRIPT_DIR"
GRADLE_CMD="$PROJECT_ROOT/gradlew"

echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   Integration Test Runner - Oracle → PostgreSQL Reconciliation  ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}\n"

# Parse arguments
DEBUG=false
CLEAN=false
COMPILE_ONLY=false

for arg in "$@"; do
    case $arg in
        --debug)
            DEBUG=true
            ;;
        --clean)
            CLEAN=true
            ;;
        --compile-only)
            COMPILE_ONLY=true
            ;;
        *)
            echo "Unknown option: $arg"
            echo "Usage: $0 [--debug] [--clean] [--compile-only]"
            exit 1
            ;;
    esac
done

# Function to print section headers
print_section() {
    echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
}

# Function to check command existence
check_command() {
    if ! command -v $1 &> /dev/null; then
        echo -e "${RED}✗ Required command not found: $1${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Found: $1${NC}"
}

# Function to check Docker
check_docker() {
    print_section "Checking Docker Setup"
    
    check_command docker
    check_command java
    
    # Check Docker daemon
    echo -e "\nVerifying Docker daemon..."
    if ! docker ps > /dev/null 2>&1; then
        echo -e "${RED}✗ Docker daemon not accessible${NC}"
        echo -e "${YELLOW}Please ensure Docker Desktop is running${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Docker daemon accessible${NC}"
    
    # Check Docker socket
    echo -e "\nChecking Docker socket..."
    if [ ! -e /var/run/docker.sock ]; then
        echo -e "${RED}✗ Docker socket not found at /var/run/docker.sock${NC}"
        exit 1
    fi
    if [ ! -r /var/run/docker.sock ]; then
        echo -e "${RED}✗ Docker socket not readable${NC}"
        exit 1
    fi
    if [ ! -w /var/run/docker.sock ]; then
        echo -e "${RED}✗ Docker socket not writable${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Docker socket accessible at /var/run/docker.sock${NC}"
    
    # Show Docker info
    echo -e "\n${YELLOW}Docker Configuration:${NC}"
    docker info | grep -E "Server Version|Storage Driver|Runtimes" | sed 's/^/  /'
}

# Function to compile project
compile_project() {
    print_section "Compiling Project"
    
    echo "Running: $GRADLE_CMD clean compileJava compileTestJava"
    $GRADLE_CMD clean compileJava compileTestJava --info 2>&1 | tail -20
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Compilation successful${NC}"
    else
        echo -e "${RED}✗ Compilation failed${NC}"
        exit 1
    fi
}

# Function to get classpath from Gradle
get_classpath() {
    print_section "Building Classpath"
    
    # Use Gradle to print classpath
    echo "Extracting runtime classpath..."
    CLASSPATH=$($GRADLE_CMD -q printClasspath 2>/dev/null || echo "")
    
    if [ -z "$CLASSPATH" ]; then
        echo -e "${YELLOW}Gradle printClasspath task not available, using standard paths${NC}"
        CLASSPATH="$PROJECT_ROOT/build/classes/java/main:$PROJECT_ROOT/build/classes/java/test:$PROJECT_ROOT/build/resources/main:$PROJECT_ROOT/build/resources/test"
        
        # Add Gradle dependencies
        if [ -d ~/.gradle/caches ]; then
            for jar in $(find ~/.gradle/caches -name "*.jar" -path "*testcontainers*" -o -name "*.jar" -path "*junit*" 2>/dev/null | head -20); do
                CLASSPATH="$CLASSPATH:$jar"
            done
        fi
    fi
    
    export CLASSPATH="$CLASSPATH"
    echo -e "${GREEN}✓ Classpath configured${NC}"
    
    if [ "$DEBUG" = true ]; then
        echo -e "\n${YELLOW}Classpath entries:${NC}"
        echo "$CLASSPATH" | tr ':' '\n' | head -20
        if [ $(echo "$CLASSPATH" | tr ':' '\n' | wc -l) -gt 20 ]; then
            echo "  ... and $(( $(echo "$CLASSPATH" | tr ':' '\n' | wc -l) - 20 )) more"
        fi
    fi
}

# Function to run integration tests
run_tests() {
    print_section "Running Integration Tests"
    
    if [ "$COMPILE_ONLY" = true ]; then
        echo -e "${GREEN}Compilation complete. Use --compile-only=false to run tests.${NC}"
        exit 0
    fi
    
    echo "Configuring Docker environment..."
    export DOCKER_HOST="unix:///var/run/docker.sock"
    export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="/var/run/docker.sock"
    
    echo -e "\n${YELLOW}Test Configuration:${NC}"
    echo "  Docker Host: $DOCKER_HOST"
    echo "  Project Root: $PROJECT_ROOT"
    echo "  Java Version: $(java -version 2>&1 | head -1)"
    
    echo -e "\n${YELLOW}Starting Oracle container (gvenzl/oracle-xe:21-slim-faststart)...${NC}"
    echo "  This may take 1-2 minutes on first run to download image"
    
    echo -e "\n${YELLOW}Starting PostgreSQL container (postgres:15-alpine)...${NC}"
    
    # Run tests using Gradle (since we already have proper setup)
    # This is simpler than trying to set up full Java classpath manually
    echo -e "\n${YELLOW}Executing integration tests via Gradle...${NC}\n"
    
    if $GRADLE_CMD integrationTest --info 2>&1 | tee /tmp/integration-test.log; then
        print_section "Test Execution Complete"
        echo -e "${GREEN}✓ Integration tests passed${NC}\n"
        exit 0
    else
        print_section "Test Execution Failed"
        echo -e "${RED}✗ Integration tests failed${NC}"
        echo -e "\n${YELLOW}Last 50 lines of output:${NC}"
        tail -50 /tmp/integration-test.log
        exit 1
    fi
}

# Function to clean up containers
cleanup_containers() {
    print_section "Cleaning Up Containers"
    
    echo "Stopping and removing test containers..."
    docker ps -a | grep -E "oracle|postgres" | awk '{print $1}' | xargs -r docker rm -f 2>/dev/null || true
    
    echo -e "${GREEN}✓ Cleanup complete${NC}"
}

# Main execution
main() {
    print_section "Pre-flight Checks"
    check_docker
    
    if [ "$CLEAN" = true ]; then
        cleanup_containers
    fi
    
    compile_project
    get_classpath
    run_tests
}

# Run main function
main
