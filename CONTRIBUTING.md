# Contributing

This document is the **technical** part of DTU Event documentation, for general users. For developer and contributor documentation, see [README.md](./README.md).

We had previously created a version of UniEvent called DTUEvent hosted on Google Cloud and Firebase. Now, we wish to rename the site (to avoid trademark violations) and host it on our own remote hosted server backend, plus add features, e.g. login. 

## Tech Stack

UniEventServer currently runs as a Java/Spring backend with Dockerized infrastructure services.

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

## TODO

### Big Tasks
- [ ] Port /web frontend to backend
- [ ] Facebook Page Organizer Onboarding Flow
- [ ] Fix (auto) token refresh if possible lol

### Serverless Functions
- [ ] FB Callback
- [ ] FB -> DB Ingest
- [ ] Tokens

### Security
- [ ] Add authentication & authorization - all API endpoints are currently public lol
- [ ] Configure CORS
- [ ] Restrict `/actuator/health` details (currently exposes DB/memory info publicly lol)

### Database
- [ ] Replace `ddl-auto: update` with Flyway or Liquibase migrations
- [ ] Fix N+1 query problem with JOIN FETCH in repositories
- [ ] Implement pagination for large result sets
- [ ] Add cascade delete / orphan removal for page deletion
- [ ] Disable `show-sql` in production profile
- [x] SeaweedFS media volume is lost when Docker is downed, so...

### API
- [ ] Add missing HTTP methods: `updateEvent`, `deleteEvent`, etc.
- [ ] Add SLF4J logging throughout services (im not sure what this is lol but claude suggested it)

### Testing
- [ ] Add Testcontainers for integration tests (currently uses H2, not real MySQL)
- [ ] Add `mvn test` step to CI pipeline before deploying
- [ ] Add rollback mechanism to deploy script

### Frontend
- [ ] User favorites and personalization
- [ ] FB authentication (Facebook OAuth)
- [ ] Business Manager integration for stable API access
- [ ] Page admin dashboard for managing event sync

### Nice-To-Have
- [ ] Make more mobile-friendly
- [ ] Location mapping
- [ ] Manual event submission (fallback for pages without dedicated event)
- [ ] Event categorization (academic, social, etc.)
- [ ] Event moderation and approval system
- [ ] Analytics dashboard for event engagement
- [ ] Notification system for page admins (event sync status, token expiration)

## Project Structure

```
UniEventServer/
├── .github/workflows/
│   └── deploy-live.yml          # CI/CD: SSH deploy to production on push to `live`
├── deploy/
│   ├── nginx.conf               # Nginx: production HTTP (ACME challenge support)
│   ├── nginx-https.conf         # Nginx: production HTTPS (TLS 1.2+, HSTS, security headers)
│   └── nginx-dev.conf           # Nginx: local dev HTTPS with self-signed cert
├── vault/
│   └── config/
│       ├── vault.hcl            # HashiCorp Vault server config (file storage, TCP listener)
│       └── policies/
│           └── unievent-app.hcl # Vault policy: read-only access to secret/data/unievent
├── certs/                       # Local dev self-signed TLS certs (gitignored)
│   ├── fullchain.pem
│   └── privkey.pem
├── src/
│   ├── main/
│   │   ├── java/dk/unievent/app/
│   │   │   ├── WebApplication.java            # Spring Boot entry point
│   │   │   ├── core/
│   │   │   │   ├── config/
│   │   │   │   │   └── OpenApiConfig.java     # Swagger / SpringDoc setup
│   │   │   │   ├── controller/
│   │   │   │   │   ├── EventController.java
│   │   │   │   │   ├── PageController.java
│   │   │   │   │   └── PlaceController.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── EventDTO.java
│   │   │   │   │   ├── LocationDTO.java
│   │   │   │   │   ├── PageDTO.java
│   │   │   │   │   └── PlaceDTO.java
│   │   │   │   ├── handler/
│   │   │   │   │   └── GlobalExceptionHandler.java
│   │   │   │   ├── mapper/
│   │   │   │   │   ├── EventMapper.java
│   │   │   │   │   ├── PageMapper.java
│   │   │   │   │   └── PlaceMapper.java
│   │   │   │   └── service/
│   │   │   │       ├── EventService.java
│   │   │   │       ├── PageService.java
│   │   │   │       └── PlaceService.java
│   │   │   ├── facebook/
│   │   │   │   └── SecurityConfig.java        # OAuth2/Facebook config (not yet active)
│   │   │   ├── mysql/
│   │   │   │   ├── model/
│   │   │   │   │   ├── EventEntity.java
│   │   │   │   │   ├── MediaEntity.java
│   │   │   │   │   ├── PageEntity.java
│   │   │   │   │   └── PlaceEntity.java
│   │   │   │   └── repository/
│   │   │   │       ├── EventRepository.java
│   │   │   │       ├── MediaRepository.java
│   │   │   │       ├── PageRepository.java
│   │   │   │       └── PlaceRepository.java
│   │   │   ├── seaweedfs/
│   │   │   │   ├── MediaConfig.java
│   │   │   │   ├── MediaController.java
│   │   │   │   └── MediaService.java
│   │   │   └── vault/
│   │   │       ├── VaultClient.java           # HashiCorp Vault HTTP client
│   │   │       └── VaultProperties.java
│   │   └── resources/
│   │       ├── application.yaml              # Root config (imports the below)
│   │       ├── mysql.yaml                    # Datasource + JPA settings
│   │       ├── vault.yaml                    # Vault connection settings
│   │       ├── seaweedfs.yaml                # SeaweedFS master/volume URLs
│   │       └── facebook.yaml                 # OAuth2 config (not yet active)
│   └── test/
│       ├── java/dk/unievent/app/
│       │   ├── WebApplicationTests.java
│       │   ├── dto/                           # DTO validation unit tests
│       │   ├── integration/                   # Full API integration tests
│       │   ├── mapper/                        # Mapper unit tests
│       │   ├── repository/                    # Repository query tests
│       │   ├── service/                       # Service unit tests
│       │   └── vault/                         # Vault client tests
│       └── resources/
│           ├── application-test.yaml         # Test root config
│           ├── mysql-test.yaml               # H2 in-memory DB (MySQL mode)
│           └── vault-test.yaml               # Vault disabled for tests
├── docker-compose.yml                        # Full stack: app, mysql, vault, seaweedfs, nginx, certbot
├── docker-compose.override.yml               # Local dev overrides (gitignored)
├── docker-compose.override.yml.example       # Template for the above
├── Dockerfile                                # Multi-stage build: JDK build → JRE runtime
├── .env                                      # Local env vars / DB credentials (gitignored)
└── pom.xml                                   # Maven build, Java 25, Spring Boot 4.x
```
