#!/bin/bash
# Stop and remove Docker containers

echo "Stopping Docker containers..."
docker-compose down -v

echo "Containers stopped and volumes removed"
