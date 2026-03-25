#!/bin/bash

echo "Starting UniEventServer Microservices..."

# Set environment variables (customize these)
export FACEBOOK_APP_ID=${FACEBOOK_APP_ID:-"your_facebook_app_id"}
export FACEBOOK_APP_SECRET=${FACEBOOK_APP_SECRET:-"your_facebook_app_secret"}
export FACEBOOK_REDIRECT_URI=${FACEBOOK_REDIRECT_URI:-"http://localhost:8080/callback"}
export GCP_PROJECT_ID=${GCP_PROJECT_ID:-"your_gcp_project_id"}
export GCP_STORAGE_BUCKET=${GCP_STORAGE_BUCKET:-"unievent-images"}

echo "Starting Facebook Service on port 8081..."
cd facebook-service
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081 &
FACEBOOK_PID=$!
cd ..

sleep 5

echo "Starting Secret Manager Service on port 8082..."
cd secret-manager-service
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082 &
SECRET_MANAGER_PID=$!
cd ..

sleep 5

echo "Starting Storage Service on port 8083..."
cd storage-service
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8083 &
STORAGE_PID=$!
cd ..

sleep 5

echo "Starting Core Service on port 8084..."
cd core-service
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8084 &
CORE_PID=$!
cd ..

sleep 5

echo "Starting Main Service on port 8080..."
mvn spring-boot:run

# Cleanup function
cleanup() {
    echo "Stopping services..."
    kill $FACEBOOK_PID 2>/dev/null
    kill $SECRET_MANAGER_PID 2>/dev/null
    kill $STORAGE_PID 2>/dev/null
    kill $CORE_PID 2>/dev/null
    exit 0
}

# Set trap to cleanup on script exit
trap cleanup SIGINT SIGTERM

echo "All services started! Press Ctrl+C to stop all services."
wait