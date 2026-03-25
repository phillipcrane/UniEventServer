# Architecture

## Overview

UniEventServer is split into focused Spring Boot services. The web service orchestrates business flow, while integration-heavy concerns are delegated to dedicated microservices.

## Services

### web (port 8080)

Responsibilities:
- Handles API entrypoints used by clients.
- Coordinates OAuth, ingestion, and token refresh workflows.
- Persists `Page` and `Event` entities in MySQL.

Does not own:
- Facebook Graph API protocol details.
- Token secret persistence implementation.
- Object storage implementation.

### facebook-service (port 8081)

Responsibilities:
- Encapsulates Facebook Graph API calls.
- Exchanges short-lived and long-lived OAuth tokens.
- Retrieves user pages and page events.

### secret-manager-service (port 8082)

Responsibilities:
- Stores and retrieves page tokens in Google Secret Manager.
- Provides token upsert and read APIs.

### storage-service (port 8083)

Responsibilities:
- Stores and deletes image objects in Google Cloud Storage.
- Handles direct byte upload and multipart upload endpoints.
- Exposes a placeholder URL-ingestion endpoint for future implementation.

### core-service (port 8084)

Responsibilities:
- Exposes core service metadata and discovery endpoints.
- Provides capability/health contract for operations and integration checks.

Does not own:
- Token storage endpoints.
- Blob/object storage endpoints.

## Service Communication

Primary flow:
1. Client calls web service.
2. Web service calls facebook-service for OAuth and event retrieval.
3. Web service calls secret-manager-service for token persistence.
4. Web service calls storage-service for image operations.
5. Core-service provides service-level metadata (`/api/core/capabilities`).

## Configuration Contract

The web service expects these URL properties:
- `services.facebook.url`
- `services.secret-manager.url`
- `services.storage.url`
- `services.core.url`

Environment overrides are supported through:
- `FACEBOOK_SERVICE_URL`
- `SECRET_MANAGER_SERVICE_URL`
- `STORAGE_SERVICE_URL`
- `CORE_SERVICE_URL`
