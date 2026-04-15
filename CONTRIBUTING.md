# Contributing
This document is the **technical** part of DTU Event documentation, for general users. For developer and contributor documentation, see [README.md](./README.md).

We had previously created a version of UniEvent called DTUEvent hosted on Google Cloud and Firebase. Now, we wish to rename the site (to avoid trademark violations) and host it on our own remote hosted server backend, plus add features, e.g. login. 

## Tech Stack

UniEventServer currently runs as a Java/Spring backend with Dockerized infrastructure services on a remote host.

- **Java** (Language, Runtime)
- **Maven** (Build Tool)
- **Spring Boot** (Application Development Framework)
  - **Spring Web** (REST API)
	- **Spring Data JPA** (database access)
	- **Spring Validation** (DTO validation)
	- **Spring OAuth2 Client** (Facebook auth flow groundwork)
	- **Spring Actuator** (health/ops endpoints)
- **Lombok** (Boilerplate reduction)
- **MySQL** (Relational Database)
- **JPA/Hibernate** (ORM)
- **H2** (Testing)
- **Docker** (Containerization)
- **SeaweedFS** (Media Storage)
- **HashiCorp Vault** (Secret Storage)

## Setup

Run `./tools setup`. It will:
- Check check whether you have installed the right dependencies (Java, Maven, cURL, Docker)
- Check for an `.env` file in root. If not there, ask from dev team.
- Set up HTTPS/TLS self-certs by creating files (`docker-compose.override.yml`, OpenSSL certs in `/certs`)
- Add "tools" environmental variable (Windows: As user PATH in the directory ~/.local/bin) so you no longer have to prefix the dev tools with `./setup`, but now just with `setup`
- Optionally begin Docker Compose

## TODO

### Backend Big Tasks
- [in progress] Implement real JWT auth (signed token, expiry, validation filter) and protect non-public API routes
- [ ] Port /web directory in old repo to new repo (entire frontend currently missing)
- [ ] Mount frontend dist as volume to avoid having to --build when changes are made
- [ ] Create Page functionality. Also Split create/update logic so `PUT /api/pages/{id}` returns `404` when page does not exist. Prevent client-controlled ID abuse on create flows (server-side ID policy and validation)
- [in progress] Fix (auto) token refresh if possible lol
- [ ] Proper admin tool framework 

### Frontend Big Tasks
- [in progress] Facebook Page Organizer Onboarding Flow
- [ ] Make more mobile-friendly
- [ ] FB authentication (Facebook OAuth)
- [ ] Page admin dashboard for managing event sync
- [ ] Create Event Page
- [ ] Business Manager integration for stable API access
- [ ] User favorites and personalization

### Dev Tools
- [ ] Finish setup with Vault seal/unseal
- [ ] Easy Seal/Unseal tool
- [ ] Manual Ingest tool
- [ ] Manual Token Refresh tool
- [ ] Check Health tool
- [ ] Refactor away from infra/seeding directory 
- [ ] Put SeedController, SeedReponse and SeedService into a flat infra/tools/seeding folder

### DB
- [ ] Align place deletion behavior with docs (nullify place in events OR document cascading delete)
- [ ] Add deterministic media replacement lifecycle (cleanup old DB record and SeaweedFS file on replace)

### Serverless Functions
- [ ] FB Callback
- [ ] FB -> DB Ingest
- [ ] Tokens

### API
- [x] Restrict `/admin/seed` endpoints to local/dev only (profile and/or role guard)
- [x] Fix page/place search to do true partial matching (`contains`/`like`) instead of exact match
- [ ] Add integration tests for auth guardrails, seed endpoint access control, and update-not-found behavior
- [ ] Make actuator `health/info` access strategy explicit for Docker probes + production security

### Small Tasks
- [ ] Move JWT refresh in ms env var to JwtConfig
- [ ] Consolidate constants into one config called ConstantsConfig
- [ ] Replace field injection with constructor injection for consistency and testability
- [ ] Harden media download content-type handling with safe fallback on invalid metadata
- [ ] Refactor away from FacebookAppSecurityUtil; just do it directly in the code
- [ ] Combine RestClientSecurityConfig into RestClientConfig
- [ ] Add more exceptions where needed

### Nice-To-Have Frameworks
- [ ] DB Replace `ddl-auto: update` with Flyway or Liquibase migrations
- [ ] Quartz scheduler
- [ ] PicoCLI for proper tool CLI integration
- [ ] Pin Docker image versions (avoid `latest` tags for reproducibility)

### Nice-To-Have Features
- [ ] Location mapping (A literal google maps with the events placed)
- [ ] Manual event submission (fallback for pages without dedicated event)
- [ ] Event categorization (academic, social, etc.)
- [ ] Event moderation and approval system
- [ ] Analytics dashboard for event engagement
- [ ] Notification system for page admins (event sync status, token expiration)
- [ ] Save likes to local-storage which can be transferred upon login (possibly with a dilogue box asking you "You have saved likes while not logged in. Would you like to transfer them?")

### Testing
- [ ] Add `AuthControllerTests` for register/login success, validation errors, and invalid credentials
- [ ] Add `JwtServiceTests` for token generation format, uniqueness, and edge-case inputs
- [ ] Add `UserServiceTests` for duplicate email/username rejection, password encoding, and lookup failures
- [ ] Add `UserRepositoryTests` and `UserEntityTests` for persistence/constraints coverage
- [ ] Add `SeedDataControllerTests` (dev profile exposure, success/error status mapping for POST/DELETE)
- [ ] Add `DataSeederServiceTests` (seed and clear behavior, `SEED_` filtering safety, error paths)
- [ ] Add repository/integration tests for true partial search semantics (contains/like) for pages and places
- [ ] Add integration tests for `PUT /api/pages/{id}` returning `404` when page does not exist
- [ ] Add media replacement lifecycle tests (old DB record + SeaweedFS cleanup on replace)
- [ ] Add config tests for `RestClientConfig` and `PaginationConstantsConfig`
- [ ] Fix `PageControllerTests.getPageByIdShouldReturnNotFoundWhenMissing` to use `Optional.empty()` (not `null`)

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
├── .mvn/
│   └── wrapper/
├── .vscode/
├── CONTRIBUTING.md
├── certs/ (autogenerated)
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
│   │   │   │   ├── dto/
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
│   │   │       ├── config/
│   │   │       └── seeding/
│   │   └── resources/
│   │       ├── api.yaml
│   │       ├── application-dev.yaml
│   │       ├── application.yaml
│   │       ├── db.yaml
│   │       ├── media.yaml
│   │       └── vault.yaml
│   └── test/
│       ├── java/
│       └── resources/
├── target/
└── vault/
    └── config/
```
