# Contributing

This document is the **technical** part of DTU Event documentation, for general users. For developer and contributor documentation, see [README.md](./README.md).

We had previously created a version of UniEvent called DTUEvent hosted on Google Cloud and Firebase. Now, we wish to rename the site (to avoid trademark violations) and host it on our own remote hosted server backend, plus add features, e.g. login. 

## TODO

### Big Tasks
- [ ] Port /web directory in old repo to new repo (entire frontend currently missing)
- [ ] Facebook Page Organizer Onboarding Flow
- [ ] Fix (auto) token refresh if possible lol

### Serverless Functions
- [ ] FB Callback
- [ ] FB -> DB Ingest
- [ ] Tokens

### Database

- [ ] Implement pagination for large result sets

### API
- [x] Add SLF4J logging throughout services

### Frontend
- [ ] User favorites and personalization
- [ ] FB authentication (Facebook OAuth)
- [ ] Business Manager integration for stable API access
- [ ] Page admin dashboard for managing event sync

### Nice-To-Have
- [ ] DB Replace `ddl-auto: update` with Flyway or Liquibase migrations
- [ ] Make more mobile-friendly
- [ ] Location mapping (A literal google maps with the events placed)
- [ ] Manual event submission (fallback for pages without dedicated event)
- [ ] Event categorization (academic, social, etc.)
- [ ] Event moderation and approval system
- [ ] Analytics dashboard for event engagement
- [ ] Notification system for page admins (event sync status, token expiration)

## Tech Stack

UniEventServer currently runs as a Java/Spring backend with Dockerized infrastructure services on a remote host.

- Java (Language, Runtime)
- Maven (Build Tool)
- Spring Boot (Application Development Framework)
    - Spring Web (REST API)
	- Spring Data JPA (database access)
	- Spring Validation (DTO validation)
	- Spring OAuth2 Client (Facebook auth flow groundwork)
	- Spring Actuator (health/ops endpoints)
- Lombok (Boilerplate reduction)
- MySQL (Relational Database)
- JPA/Hibernate (ORM)
- H2 (Testing)
- Docker (Containerization)
- SeaweedFS (Media Storage)
- HashiCorp Vault (Secret Storage)

## Setup

1. Get .env file and place in root
2. Copy and rename the override file: `cp docker-compose.override.yml.example docker-compose.override.yml`
3. Generate a self-signed cert: `mkdir -p certs && openssl req -x509 -nodes -days 3650 -newkey rsa:2048 -keyout certs/privkey.pem -out certs/fullchain.pem -subj "/CN=localhost"`
4. Start the stack: `docker compose up -d`

## Debugging & Logs

### Viewing Application Logs

The application logs are configured with SLF4J/Logback and output to both console and file:

**View logs in real-time from the running container:**
```powershell
docker logs -f unievent-app
```

**View logs for all services:**
```powershell
docker compose logs -f
```

**View log file inside container:**
```powershell
docker exec -it unievent-app cat logs/app.log
```

### Enabling Debug Logging

By default, application logs are at INFO level (production safe). To enable DEBUG logging for development:

**Start the stack with debug profile:**
```powershell
docker compose down
$env:SPRING_PROFILES_ACTIVE = "dev"
docker compose up -d --build
```

**Or on Linux/Mac:**
```bash
docker compose down
SPRING_PROFILES_ACTIVE=dev docker compose up -d --build
```

Debug logging provides additional detail on:
- Database queries and pagination
- API endpoint entry/exit points
- Infrastructure client operations (SeaweedFS, Vault)

Debug output is controlled by `src/main/resources/application-dev.yaml` and is automatically disabled when the dev profile is not active.

### Disabling Debug Logging

To return to INFO-level logging:

**Windows PowerShell:**
```powershell
docker compose down
docker compose up -d --build
```

**Linux/Mac:**
```bash
docker compose down
docker compose up -d --build
```

Simply restart without the `SPRING_PROFILES_ACTIVE=dev` variable to return to production-safe INFO-level logging.

## Endpoints

**Read:**
| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/events` | List all events. |
| `GET` | `/api/events/future` | List upcoming events only. |
| `GET` | `/api/events/{id}` | Fetch one event by ID. |
| `GET` | `/api/events/page/{pageId}` | Events for one page. |
| `GET` | `/api/events/page/{pageId}/future` | Upcoming events for one page. |
| `GET` | `/api/events/place/{placeId}` | Events at one place. |
| `GET` | `/api/pages` | List all pages. |
| `GET` | `/api/pages/active` | List active pages only. |
| `GET` | `/api/pages/{id}` | Fetch one page by ID. |
| `GET` | `/api/pages/search` | Search pages by query. |
| `GET` | `/api/places/{id}` | Fetch one place by ID. |
| `GET` | `/api/places/city/{city}` | List places in a city. |
| `GET` | `/api/places/country/{country}` | List places in a country. |
| `GET` | `/api/places/location/{city}/{country}` | Filter places by city and country. |
| `GET` | `/api/places/search` | Search places by query. |

**Write:**
| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/events` | Create an event. |
| `PUT` | `/api/events/{id}` | Update an event. |
| `POST` | `/api/events/{id}/coverImage` | Upload or replace the event cover image. |
| `DELETE` | `/api/events/{id}` | Delete an event. |
| `POST` | `/api/pages` | Create a page. |
| `PUT` | `/api/pages/{id}` | Update a page. |
| `POST` | `/api/pages/{id}/picture` | Upload or replace the page picture. |
| `DELETE` | `/api/pages/{id}` | Delete a page. |
| `POST` | `/api/places` | Create a place. |
| `PUT` | `/api/places/{id}` | Update a place. |
| `DELETE` | `/api/places/{id}` | Delete a place. |

## Project Structure

```text
UniEventServer/
├── .dockerignore
├── .gitattributes
├── .github/
│   └── workflows/
│       └── deploy-live.yml
├── .gitignore
├── .idea/
├── .vscode/
├── CONTRIBUTING.md
├── deploy/
│   ├── nginx.conf
│   ├── nginx-dev.conf
│   └── nginx-https.conf
├── docker-compose.override.yml
├── docker-compose.override.yml.example
├── docker-compose.yml
├── Dockerfile
├── LICENSE
├── mvnw
├── mvnw.cmd
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/dk/unievent/app/
│   │   │   ├── WebApplication.java
│   │   │   ├── api/
│   │   │   │   ├── controller/
│   │   │   │   └── handler/
│   │   │   ├── application/
│   │   │   │   ├── dto/
│   │   │   │   ├── mapper/
│   │   │   │   └── service/
│   │   │   ├── db/
│   │   │   │   ├── model/
│   │   │   │   └── repository/
│   │   │   └── infrastructure/
│   │   │       ├── client/
│   │   │       └── config/
│   │   └── resources/
│   │       ├── api.yaml
│   │       ├── application.yaml
│   │       ├── db.yaml
│   │       ├── media.yaml
│   │       └── vault.yaml
│   └── test/
└── vault/
    └── config/
```
