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

Run `pwsh ./tools.ps1 setup`. If you're on Mac/Linux, install PowerShell and restart terminal (login and logout/restart computer if that doesn't work). The command will:
- Check whether you have installed the right dependencies (Java, Maven, cURL, Docker)
- Check for an `.env` file in root. If not there, ask from dev team.
- Set up HTTPS/TLS self-certs by creating files (`docker-compose.override.yml`, OpenSSL certs in `/certs`)
- Add the `tools` command to your PATH (Windows: user PATH includes `~/.local/bin`) so you can run commands directly (for example `tools setup`)
- Optionally begin Docker Compose

## Tools

After running `pwsh ./tools.ps1 setup`, you will be able to type `tools` to run the following commands from our cli folder:

```text
cli/
├── setup.ps1      # tools setup
├── ingest.ps1     # tools ingest [-p <pageId>]
├── refresh.ps1    # tools refresh [-p <pageId>]
├── seed.ps1       # tools seed, tools seed --wipe
├── vault.ps1      # tools vault, tools unseal
└── shared.ps1     # (helpers)
```

## TODO

### Backend Big Tasks
- [in progress] Implement real JWT auth (signed token, expiry, validation filter) and protect non-public API routes
- [X] Port /web directory in old repo to new repo (entire frontend currently missing)
- [X] Mount frontend dist as volume to avoid having to --build when changes are made
- [x] Split create/update logic so `PUT /api/pages/{id}` returns `404` when page does not exist
- [in progress] Fix (auto) token refresh if possible lol
- [x] Proper admin tool framework 

### Frontend Big Tasks
- [in progress] Facebook Page Organizer Onboarding Flow
- [in progress] Make more mobile-friendly
- [ ] FB authentication (Facebook OAuth)
- [ ] Page admin dashboard for managing event sync
- [ ] Create Event Page
- [ ] Business Manager integration for stable API access
- [ ] User favorites and personalization

### Serverless Functions
- [X] FB Callback
- [X] FB -> DB Ingest
- [ ] Tokens

### Nice-To-Have Frameworks
- [ ] DB Replace `ddl-auto: update` with Flyway or Liquibase migrations (move to `ddl-auto: validate`)
- [ ] Quartz scheduler
- [ ] PicoCLI for proper tool CLI integration
- [ ] Pin Docker image versions (avoid `latest` tags for reproducibility)

### Nice-To-Have Features
- [ ] Location mapping (A literal google maps with the events placed)
- [ ] Manual event submission (fallback for pages without dedicated event). Includes backend Create Page functionality. Prevent client-controlled ID abuse on create flows (server-side ID policy and validation)
- [ ] Event categorization (academic, social, etc.)
- [ ] Event moderation and approval system
- [ ] Analytics dashboard for event engagement
- [ ] Notification system for page admins (event sync status, token expiration)
- [ ] Save likes to local-storage which can be transferred upon login (possibly with a dilogue box asking you "You have saved likes while not logged in. Would you like to transfer them?")

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
├── certs/ (autogenerated)
├── CONTRIBUTING.md
├── deploy/
│   ├── nginx-dev.conf
│   ├── nginx-https.conf
│   └── nginx.conf
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
│   │   │   ├── api/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   └── handler/
│   │   │   ├── application/
│   │   │   │   ├── dto/
│   │   │   │   ├── mapper/
│   │   │   │   ├── scheduler/
│   │   │   │   └── service/
│   │   │   ├── db/
│   │   │   │   ├── model/
│   │   │   │   └── repository/
│   │   │   ├── infrastructure/
│   │   │   │   ├── client/
│   │   │   │   ├── config/
│   │   │   │   ├── exception/
│   │   │   │   ├── filter/
│   │   │   │   └── security/
│   │   │   ├── tools/
│   │   │   │   ├── controller/
│   │   │   │   ├── models/
│   │   │   │   └── services/
│   │   └── resources/
│   │       ├── api.yaml
│   │       ├── application-dev.yaml
│   │       ├── application.yaml
│   │       ├── db.yaml
│   │       ├── media.yaml
│   │       ├── templates/
│   │       │   └── emails/
│   │       └── vault.yaml
│   └── test/
│       ├── java/dk/unievent/app/
│       │   ├── api/
│       │   │   ├── controller/
│       │   │   └── handler/
│       │   ├── application/
│       │   │   ├── dto/
│       │   │   ├── mapper/
│       │   │   └── service/
│       │   ├── db/
│       │   │   ├── model/
│       │   │   └── repository/
│       │   ├── infrastructure/
│       │   │   ├── client/
│       │   │   ├── config/
│       │   │   ├── exception/
│       │   │   └── filter/
│       │   └── tools/
│       │       ├── controller/
│       │       └── services/
│       └── resources/
│           ├── application-test.yaml
│           ├── db-test.yaml
│           ├── logback-test.xml
│           └── vault-test.yaml
├── target/
└── vault/
    └── config/
```
