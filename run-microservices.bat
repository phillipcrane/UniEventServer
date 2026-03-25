@echo off
echo Starting UniEventServer Microservices...

echo Setting environment variables...
set FACEBOOK_APP_ID=%FACEBOOK_APP_ID%
set FACEBOOK_APP_SECRET=%FACEBOOK_APP_SECRET%
set FACEBOOK_REDIRECT_URI=%FACEBOOK_REDIRECT_URI%
set GCP_PROJECT_ID=%GCP_PROJECT_ID%
set GCP_STORAGE_BUCKET=%GCP_STORAGE_BUCKET%

echo Starting Facebook Service on port 8081...
start "Facebook Service" cmd /c "cd facebook-service && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081"

timeout /t 5 /nobreak > nul

echo Starting Secret Manager Service on port 8082...
start "Secret Manager Service" cmd /c "cd secret-manager-service && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082"

timeout /t 5 /nobreak > nul

echo Starting Storage Service on port 8083...
start "Storage Service" cmd /c "cd storage-service && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8083"

timeout /t 5 /nobreak > nul

echo Starting Core Service on port 8084...
start "Core Service" cmd /c "cd core-service && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8084"

timeout /t 5 /nobreak > nul

echo Starting Main Service on port 8080...
mvn spring-boot:run

echo All services started!