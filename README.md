# UniEventServer - Microservices Architecture

This project is structured as independent Spring Boot microservices that can run separately for better scalability and maintainability.

## Architecture Overview

The application is split into five independent services:

1. **Main Service** (Port 8080) - Event management and orchestration
2. **Facebook Service** (Port 8081) - Facebook API integration
3. **Secret Manager Service** (Port 8082) - GCP Secret Manager operations
4. **Storage Service** (Port 8083) - GCP Cloud Storage operations
5. **Core Service** (Port 8084) - Core metadata and service discovery

Additionally, this repository now includes a Node.js serverless package in `functions/`.
That package mirrors the UniEvent serverless API layer and can be run/tested independently
for endpoint parity and migration support.

## Service Descriptions

### Main Service (Port 8080)
- **Purpose**: Orchestrates event ingestion and management
- **Endpoints**:
  - `POST /api/callback` - Handle Facebook OAuth callback
  - `POST /api/ingest` - Ingest events from all connected pages
  - `POST /api/refresh-tokens` - Refresh all page access tokens
- **Dependencies**: MySQL database, calls other microservices

### Facebook Service (Port 8081)
- **Purpose**: Handle all Facebook Graph API interactions
- **Endpoints**:
  - `POST /api/facebook/oauth/token` - Get short-lived access token
  - `POST /api/facebook/oauth/long-lived-token` - Exchange for long-lived token
  - `GET /api/facebook/user/pages` - Get user's Facebook pages
  - `GET /api/facebook/pages/{pageId}/events` - Get events for a page
  - `POST /api/facebook/oauth/refresh` - Refresh page access token

### Secret Manager Service (Port 8082)
- **Purpose**: Manage Facebook page access tokens in GCP Secret Manager
- **Endpoints**:
  - `POST /api/secrets/pages/{pageId}/token` - Store page token
  - `PUT /api/secrets/pages/{pageId}/token` - Update page token
  - `GET /api/secrets/pages/{pageId}/token` - Retrieve page token
  - `GET /api/secrets/pages/{pageId}/token/status` - Check token expiry

### Storage Service (Port 8083)
- **Purpose**: Handle image storage in GCP Cloud Storage
- **Endpoints**:
  - `POST /api/storage/images` - Upload image data
  - `POST /api/storage/images/upload` - Upload multipart file
  - `POST /api/storage/images/from-url` - Placeholder acknowledgement endpoint for URL ingestion
  - `GET /api/storage/images/{filePath}` - Get image info
  - `DELETE /api/storage/images/{filePath}` - Delete image

### Core Service (Port 8084)
- **Purpose**: Expose orchestration metadata and service discovery
- **Endpoints**:
  - `GET /api/core/health` - Core service health snapshot
  - `GET /api/core/capabilities` - Current downstream service URL contract

## Prerequisites

- Java 25
- Maven 3.6+
- MySQL 8.0+
- GCP Project with:
  - Secret Manager API enabled
  - Cloud Storage API enabled
  - Service account with appropriate permissions

## Environment Variables

Set the following environment variables:

```bash
# Facebook API
export FACEBOOK_APP_ID=your_facebook_app_id
export FACEBOOK_APP_SECRET=your_facebook_app_secret
export FACEBOOK_REDIRECT_URI=http://localhost:8080/callback

# GCP Configuration
export GCP_PROJECT_ID=your_gcp_project_id
export GCP_STORAGE_BUCKET=your_storage_bucket_name

# Optional: Override service URLs
export FACEBOOK_SERVICE_URL=http://localhost:8081
export SECRET_MANAGER_SERVICE_URL=http://localhost:8082
export STORAGE_SERVICE_URL=http://localhost:8083
export CORE_SERVICE_URL=http://localhost:8084
```

## Running the Services

### Option 1: Run All Services Together

Use the provided script to start all services:

```bash
# Linux/Mac
./run-microservices.sh

# Windows
run-microservices.bat
```

### Option 2: Run Services Individually

Start each service in separate terminals:

```bash
# Terminal 1: Facebook Service
cd facebook-service
mvn spring-boot:run

# Terminal 2: Secret Manager Service
cd secret-manager-service
mvn spring-boot:run

# Terminal 3: Storage Service
cd storage-service
mvn spring-boot:run

# Terminal 4: Core Service
cd ../core-service
mvn spring-boot:run

# Terminal 5: Main Service
cd ..
mvn spring-boot:run
```

### Option 3: Using Docker Compose

```bash
docker-compose up --build
```

## Service Communication

Services communicate via HTTP REST APIs:

- Main Service → Facebook Service: OAuth and event data
- Main Service → Secret Manager Service: Token storage/retrieval
- Main Service → Storage Service: Image upload/download
- Core Service → exposes central capability metadata for ops and integrations
- All services are stateless and independently scalable

## Project Documentation

- Architecture details: `docs/ARCHITECTURE.md`
- Endpoint inventory: `docs/API.md`
- Environment template: `.env.example`
- Node serverless package: `functions/`

## Node Serverless Package (`functions/`)

The `functions/` module contains a TypeScript/Express implementation of the same
event orchestration flow with typed endpoint contracts and endpoint bindings.

Useful commands:

```bash
cd functions
npm install
npm run build
npm test -- --run
npm run start
```

## Database Setup

The Main Service requires MySQL with the following tables:
- `pages` - Facebook page information
- `events` - Event data

Tables are created automatically via JPA/Hibernate.

## Health Checks

Each service exposes health endpoints:
- `GET /actuator/health` - Service health status

## API Documentation

Each service provides OpenAPI/Swagger documentation at:
- `http://localhost:{port}/swagger-ui.html`

## Development

### Adding New Endpoints

1. Add endpoint to the appropriate service
2. Update the Main Service to call the new endpoint
3. Update environment variables if needed
4. Update this README

### Testing

Run tests for individual services:

```bash
# Test all services
mvn test

# Test specific service
cd facebook-service
mvn test
```

## Deployment

### Production Deployment

1. Build each service:
```bash
cd facebook-service
mvn clean package -DskipTests

cd ../secret-manager-service
mvn clean package -DskipTests

cd ../storage-service
mvn clean package -DskipTests

cd ..
mvn clean package -DskipTests
```

2. Deploy JAR files to your container orchestration platform (Kubernetes, Docker Swarm, etc.)

3. Configure environment variables in your deployment platform

### Docker Deployment

Build and run with Docker:

```bash
# Build images
docker build -t unievent-facebook facebook-service/
docker build -t unievent-secret-manager secret-manager-service/
docker build -t unievent-storage storage-service/
docker build -t unievent-main .

# Run containers
docker run -p 8081:8081 unievent-facebook
docker run -p 8082:8082 unievent-secret-manager
docker run -p 8083:8083 unievent-storage
docker run -p 8080:8080 unievent-main
```

## Monitoring

Each service includes Spring Boot Actuator for monitoring:
- Health checks
- Metrics
- Info endpoints

## Troubleshooting

### Common Issues

1. **Service Unavailable**: Check if all services are running and accessible
2. **Database Connection**: Verify MySQL is running and credentials are correct
3. **GCP Permissions**: Ensure service account has required GCP permissions
4. **Port Conflicts**: Verify no other applications are using ports 8080-8084

### Logs

Check service logs for detailed error information:
```bash
# View logs for a specific service
docker logs <container_name>

# Or check application logs
tail -f logs/spring.log
```

## Contributing

1. Create a feature branch
2. Make changes to appropriate service(s)
3. Update tests
4. Update this README if needed
5. Submit pull request

## License

See LICENSE file for details.