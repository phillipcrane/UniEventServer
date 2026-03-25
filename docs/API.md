# API Reference (Operational)

This document lists the operational endpoints currently used by the system.

## web (8080)

- `POST /api/callback`
  - Purpose: process Facebook OAuth code, store page tokens, persist pages.
- `POST /api/ingest`
  - Purpose: fetch page events and persist normalized event data.
- `POST /api/refresh-tokens`
  - Purpose: refresh page tokens and persist refresh metadata.

## facebook-service (8081)

- `POST /api/facebook/oauth/token`
- `POST /api/facebook/oauth/long-lived-token`
- `GET /api/facebook/user/pages`
- `GET /api/facebook/pages/{pageId}/events`
- `POST /api/facebook/oauth/refresh`

## secret-manager-service (8082)

- `POST /api/secrets/pages/{pageId}/token`
- `PUT /api/secrets/pages/{pageId}/token`
- `GET /api/secrets/pages/{pageId}/token`
- `GET /api/secrets/pages/{pageId}/token/status`

## storage-service (8083)

- `POST /api/storage/images`
- `POST /api/storage/images/upload`
- `POST /api/storage/images/from-url`
  - Note: currently a placeholder acknowledgement endpoint.
- `GET /api/storage/images/{filePath}`
- `DELETE /api/storage/images/{filePath}`

## core-service (8084)

- `GET /api/core/health`
- `GET /api/core/capabilities`
