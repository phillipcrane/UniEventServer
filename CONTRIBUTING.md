# Contributing

This document is the **technical** part of DTU Event documentation, for general users. For developer and contributor documentation, see [README.md](./README.md).

We had previously created a version of UniEvent called DTUEvent hosted on Google Cloud and Firebase. Now, we wish to rename the site (to avoid trademark violations) and host it on our own remote hosted server backend, plus add features, e.g. login. 

## TODO

### Big Tasks
- [ ] Port /web directory in old repo to new repo (entire frontend currently missing)
- [ ] Facebook Page Organizer Onboarding Flow
- [ ] Fix (auto) token refresh if possible lol
- [ ] Mount frontend dist as volume to avoid having to --build when changes are made

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

## Uploading Test Images

Before seeding test data with images, you need to upload at least one image to get a media file with ID. The first uploaded image will be assigned ID 1, which the seed script uses.

### Upload an Image

**Prepare a test image:**
- Get any `.jpg`, `.png`, or images file (or create a simple one)
- Save it locally (e.g., `test-image.jpg`)

**Upload via curl:**
```sh
curl -X POST -F "file=@path/to/test-image.jpg" http://localhost:8080/media
```

(Same command for Windows cmd, PowerShell, Linux/Mac)

**Example response:**
```json
{
  "id": 1,
  "filename": "test-image.jpg",
  "contentType": "image/jpeg",
  "fileId": "1,abc123def456",
  "uploadedAt": "2026-04-13T15:30:00Z"
}
```

The image is now stored in SeaweedFS and accessible at `http://localhost:8080/media/1`.

### Before Seeding

1. Upload your test image (gets ID 1)
2. Run the seed command — all 10 seeded events will use this image
3. Visit the frontend and verify all event cards display the image

### Alternative: Use Swagger UI

You can also upload via the interactive API documentation:

1. Open **http://localhost:8080/swagger-ui.html**
2. Find the **POST /media** endpoint (in "Media" section)
3. Click **Try it out**
4. Click **Choose File** and select your image
5. Click **Execute**
6. Note the returned media `id` — use this in seed scripts or manual linking

## Test Data Seeding

For local development and testing, you can seed the database with minimal test data using HTTP endpoints. All seeded records are marked with a `SEED_` prefix for easy identification and cleanup.

### Installing curl

If you don't have curl installed:
- **Windows:** It's built-in on Windows 10+ (use `curl` directly in cmd/PowerShell)
- **Mac:** `brew install curl`
- **Linux:** `sudo apt install curl` (Ubuntu/Debian) or `sudo yum install curl` (RedHat/CentOS)

### Seed Test Data

Insert 2 sample pages, 10 events, 2 places, and 10 media files (all sharing the same image) into your local MySQL database:

```sh
curl -X POST http://localhost:8080/admin/seed
```

(Same command for Windows cmd, PowerShell, Linux/Mac)

**Example response:**
```json
{
  "success": true,
  "message": "Seed data created successfully",
  "pageCount": 2,
  "eventCount": 10,
  "placeCount": 2
}
```

The seeded data includes:
- **Pages:** "Tech Events", "Culture Events"
- **Events:** React Workshop, Spring Boot Masterclass, Docker & Kubernetes, Jazz Night, Art Exhibition, Film Festival, etc.
- **Places:** Copenhagen, Aarhus
- **Images:** All 10 events have cover images from your existing `/media/1` endpoint
- **Relationships:** Events linked to pages and places with realistic dates (7-45 days in future)

### Clear Seeded Data

Remove all test data marked with `SEED_` prefix:

```sh
curl -X DELETE http://localhost:8080/admin/seed
```

(Same command for Windows cmd, PowerShell, Linux/Mac)
```json
{
  "success": true,
  "message": "Seed data cleared successfully",
  "pageCount": 2,
  "eventCount": 10,
  "placeCount": 2
}
```

**Important:** Only records with the `SEED_` prefix are removed, so your production or manually-created data is safe.

### Workflow Example

```sh
# Start the stack
docker compose up -d

# Wait ~40s for services to be healthy

# Seed test data
curl -X POST http://localhost:8080/admin/seed

# Clean up when done
curl -X DELETE http://localhost:8080/admin/seed
```

### How Images Work

- Each seeded event is assigned a **unique media record** in the database
- All media records point to the **same image file** in SeaweedFS (from your existing `/media/1`)
- This avoids ID bloat while ensuring each event has its own cover image reference
- On reseed, new media records are created pointing to the same image

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
