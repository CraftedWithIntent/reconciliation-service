#!/bin/bash
# Setup Gradle Wrapper for the project
# This script initializes the Gradle wrapper if not already present

set -e

echo "Setting up Gradle Wrapper..."

# Check if Gradle is installed
if ! command -v gradle &> /dev/null; then
    echo "Gradle not found. Installing Gradle wrapper..."
    # Use a minimal gradle command to initialize the wrapper
    if [ ! -d "gradle" ]; then
        mkdir -p gradle/wrapper
    fi
    
    # Download gradle-wrapper.jar and gradle-wrapper.properties
    echo "Downloading Gradle wrapper files..."
    
    # Create wrapper properties file
    cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-bin.zip
networkTimeout=10000
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF
    
    echo "Gradle wrapper properties created."
    echo ""
    echo "To complete setup, download the wrapper JAR:"
    echo "  1. Visit: https://github.com/gradle/gradle/releases/download/v8.0/gradle-8.0-bin.zip"
    echo "  2. Extract gradle-8.0/lib/gradle-wrapper.jar to gradle/wrapper/"
    echo ""
    echo "Or pull from Git LFS if available in the repository."
    
else
    echo "Gradle found. Initializing gradle wrapper..."
    gradle wrapper --gradle-version 8.0
    echo "Gradle wrapper initialized successfully!"
fi

echo ""
echo "Setup complete! You can now use:"
echo "  ./gradlew build"
echo "  ./gradlew bootRun"
echo "  ./gradlew bootJar"
