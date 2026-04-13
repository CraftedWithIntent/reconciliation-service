#!/bin/bash
# Build and run the reconciliation service with Docker Compose

set -e

echo "Building reconciliation service..."
mvn clean package -DskipTests

echo "Starting Docker containers..."
docker-compose up -d

echo "Waiting for services to be ready..."
sleep 10

echo "Checking service health..."
curl -f http://localhost:8080/api/reconcile/health || {
  echo "Service health check failed"
  exit 1
}

echo "All services started successfully!"
echo "Source DB: localhost:5432"
echo "Target DB: localhost:5433"
echo "Reconciliation API: http://localhost:8080"
