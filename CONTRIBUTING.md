# Contributing
This document is the **technical** part of DTU Event documentation, for general users. For developer and contributor documentation, see [README.md](./README.md).

## Tech Stack

Backend:
- **Java 25** - Language and runtime
- **Maven** - Build tool
- **Docker** - Containerization for the following services:
	- **Spring Boot** - Application framework (Web, Data JPA, Validation, Actuator, OAuth2 Client)
	- **Nginx** - Reverse proxy, HTTPS termination
	- **Certbot** - SSL certificate issuance and renewal
	- **MySQL** - Relational database
	- **HashiCorp Vault** - Secret storage
	- **SeaweedFS** - Media/image storage. Has a Master and Volume.
- **Spring Mail + Thymeleaf** - Email sending with HTML templates
- **Lombok** - Boilerplate reduction
- **SLF4J** - Logging facade
- **JJWT** - JWT token signing and validation
- **SpringDoc OpenAPI** - Auto-generated API docs + Swagger UI (`/swagger-ui.html`)
- **Jackson** - JSON serialization (including JSR310 for Java date/time types)
- **JPA / Hibernate** - ORM
- **H2** - Embedded DB for tests

Frontend:
- **TypeScript 5.8** - Language, a type-safe upgrade to JavaScript
- **Node.js** - Runtime environment
- **npm** - Package manager
- **React 19** - UI framework
- **Vite 7** - Build Tool
- **Tailwind CSS 4** - Styling
- **React Router v7** Routing
- **Lucide React** - Icon library
- **Vitest 4** - Test framework
- **ESLint 9** - Linting

## Setup

Run `pwsh ./tools.ps1 setup`. On Mac/Linux, install PowerShell first. The command will:
- Check required dependencies (Java, Maven, cURL, Docker)
- Check for a root `.env` file, request one from the team if missing
- Generate self-signed TLS certs and create `docker-compose.override.yml` for local HTTPS
- Add `tools` to your PATH so you can run `tools` directly instead of `./tools.ps1`.

After setup the `tools` CLI is available - see `cli/` in the project structure below. To run frontend npm commands (`npm run dev`, `npm run test` etc), `cd` into the web directory

## Project Structure

```text
UniEventServer/
├── .github/
│   └── workflows/
│       └── deploy.yml      # CI/CD: build gates + SSH deploy + docker compose up
├── cli/
│   ├── setup.ps1      # tools setup
│   ├── docker.ps1     # tools docker / tools docker -d (stop) / tools docker --wipe
│   ├── vault.ps1      # tools vault, tools unseal
│   ├── seed.ps1       # tools seed, tools seed --wipe
│   ├── ingest.ps1     # tools ingest [-p <pageId>]
│   ├── refresh.ps1    # tools refresh [-p <pageId>]
│   ├── invite.ps1     # tools invite [-e <email>] [-n <orgname>]
│   ├── status.ps1     # tools status (read-only Docker/Vault summary)
│   └── shared.ps1     # (shared helpers)
├── deploy/
│   ├── nginx-dev.conf            # Local dev
│   └── nginx-https.conf          # Production HTTPS + reverse proxy
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
│   │   │   └── tools/            # Admin CLI endpoints (@Profile("dev"))
│   │   │       ├── controller/
│   │   │       ├── models/
│   │   │       └── services/
│   │   └── resources/
│   │       ├── templates/
│   │       │   └── emails/       # Thymeleaf email templates
│   │       ├── api.yaml
│   │       ├── application.yaml
│   │       ├── application-dev.yaml
│   │       ├── db.yaml
│   │       ├── media.yaml
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
│       │   │   ├── filter/
│       │   │   └── util/
│       │   └── tools/
│       │       ├── controller/
│       │       └── services/
│       └── resources/
│           ├── application-test.yaml
│           ├── db-test.yaml
│           ├── logback-test.xml
│           └── vault-test.yaml
├── vault/
│   └── config/
│       └── policies/
├── web/
│   ├── public/
│   ├── src/
│   │   ├── components/          # Isolated UI pieces (no data fetching)
│   │   ├── context/             # React context providers (AuthContext, LikesContext, PagesContext)
│   │   ├── data/
│   │   ├── handlers/            # Use-case orchestration (coordinates services, handles side-effects)
│   │   │   ├── login.ts         # loginWithEmail use case
│   │   │   ├── signup.ts        # signupWithEmail use case
│   │   │   ├── logout.ts        # signOutCurrentUser use case
│   │   │   ├── refresh.ts       # refreshTokens use case
│   │   │   └── facebookLogin.ts # Facebook OAuth redirect use case
│   │   ├── hooks/               # Stateful logic extracted from components (prefixed use*)
│   │   ├── pages/               # Full page views (delegate state to hooks, near-pure JSX)
│   │   ├── services/            # Pure data access: getters, setters, listeners, API calls
│   │   │   ├── dal.ts           # Data Access Layer - all REST API calls
│   │   │   ├── auth.ts          # Cookie-based auth state (in-memory store + session helpers)
│   │   │   ├── facebook.ts      # Facebook OAuth URL builders
│   │   │   └── likes.ts         # Likes persistence (localStorage + in-memory cache)
│   │   ├── styles/
│   │   ├── test/
│   │   │   ├── pages/
│   │   │   └── services/
│   │   ├── utils/               # Pure helpers used across multiple files
│   │   ├── constants.ts         # All magic values (timeouts, API paths, thresholds)
│   │   ├── main.tsx             # Entry point
│   │   ├── App.tsx
│   │   ├── router.tsx           # React Router config
│   │   └── types.ts             # Shared TypeScript interfaces and domain types
│   ├── Dockerfile               # Frontend nginx image
│   ├── nginx.conf               # SPA routing (all routes → index.html)
│   ├── package.json
│   └── vite.config.ts
├── docker-compose.yml
├── docker-compose.override.yml.example
├── Dockerfile                   # Backend image
├── pom.xml
└── tools.ps1                    # Entry point for the tools CLI
```

## Conventions

### Backend

Api Layer:
- **`/controller/`** - stage endpoints/routes, handle HTTP
- **`/dto/`** - HTTP Request/Response
- **`/handler/`** - do something

Application Layer:
- **`/service/`** - business logic, tho often getters and setters to some external service
- **`/dto/`** - internal data shapes passed between services or into mappers
- **`/mapper/`** - `@Component` beans with `toDTO` / `toEntity`. 

Data Layer:
- **`/model/`** - JPA entities only. 
- **`/repository/`** - Spring Data interfaces. Queries only, no logic

Infrastructure Layer:
- **`/config/`** - Spring config beans
- **`/exception/`** - one `RuntimeException` subclass per failure case
- **`tools/`** - `@Profile("dev")` admin endpoints only; never ships to production

---

### Frontend

- **`context/`** - app-wide shared state (AuthContext, LikesContext, PagesContext). Use a context when multiple unrelated hooks need the same data and prop-drilling would be awkward
- **`components/`** - purely presentational, no fetch calls. Delegate state to `useXxx` hooks
- **`pages/`** - compose components, delegate all state to a `useXxxPage` hook; near-pure JSX
- **`hooks/`** - stateful logic extracted from REACT components; always prefix `use*`; page-level hooks named after their page (`useMainPage`, `useEventPage`, etc.)
- **`handlers/`** - one file per use case; orchestrates service calls and state mutations; no UI concerns
- **`services/`** - pure data access: in-memory state, getters/setters, listeners, raw API fetches
- **`utils/`** - pure helpers used in more than one file; no React, no side-effects
- **`types.ts`** - all TS types (remember - TS types don't exist when the program is running, unlike in Java/C#)
- **`constants.ts`** - all magic values: timeouts, thresholds, API paths, feature flags
- **`contexts/`** - app-wide state "container", used by REACT components and pages.
- **`components/`** - purely presentational, no fetch calls. Delegate state to `useXxx` hooks
- **`pages/`** - compose components, delegate all state to a `useXxxPage` hook; near-pure JSX
- **`hooks/`** - stateful logic extracted from REACT components; always prefix `use*`; page-level hooks named after their page (`useMainPage`, `useEventPage`, etc.)
- **`handlers/`** - one file per use case; orchestrates service calls and state mutations; no UI concerns
- **`services/`** - pure data access: in-memory state, getters/setters, listeners, raw API fetches
- **`utils/`** - pure helpers used in more than one file; no React, no side-effects
- **`types.ts`** - all TS types (remember - TS types don't exist when the program is running, unlike in Java/C#)
- **`constants.ts`** - all magic values: timeouts, thresholds, API paths, feature flags

## API Endpoints

**Public (no auth):**
| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/api/events` | List all events (paginated) |
| `GET` | `/api/events/future` | Upcoming events only |
| `GET` | `/api/events/{id}` | Single event |
| `GET` | `/api/events/page/{pageId}` | Events for a page |
| `GET` | `/api/events/page/{pageId}/future` | Upcoming events for a page |
| `GET` | `/api/events/place/{placeId}` | Events at a venue |
| `GET` | `/api/pages` | List all pages |
| `GET` | `/api/pages/active` | Active pages only |
| `GET` | `/api/pages/{id}` | Single page |
| `GET` | `/api/pages/search` | Search pages by name |
| `GET` | `/api/places/{id}` | Single place |
| `GET` | `/api/places/city/{city}` | Places in a city |
| `GET` | `/api/places/country/{country}` | Places in a country |
| `GET` | `/api/places/location/{city}/{country}` | Places in a city + country |
| `GET` | `/api/places/search` | Search places by name |
| `GET` | `/api/facebook/auth` | Start Facebook OAuth - returns signed state + auth URL |
| `GET` | `/api/facebook/callback` | Facebook OAuth callback - validates state, exchanges code for tokens |
| `GET` | `/api/facebook/health` | Facebook integration health check |
| `GET` | `/media/{id}` | Download media file |
| `GET` | `/media` | List all media files (paginated) |
| `POST` | `/api/auth/register` | Register user |
| `POST` | `/api/auth/login` | Login |
| `POST` | `/api/auth/refresh` | Refresh access token |
| `POST` | `/api/auth/organizer-key/verify` | Verify organizer invite key |
| `POST` | `/api/auth/register-with-key` | Register as organizer |

**Authenticated:**
| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/events` | Create event |
| `PUT` | `/api/events/{id}` | Update event |
| `DELETE` | `/api/events/{id}` | Delete event |
| `POST` | `/api/events/{id}/coverImage` | Upload cover image |
| `POST` | `/api/pages` | Create page |
| `PUT` | `/api/pages/{id}` | Update page |
| `POST` | `/api/pages/{id}/picture` | Upload page picture |
| `DELETE` | `/api/pages/{id}` | Delete page (cascades to events) |
| `POST` | `/api/places` | Create place |
| `PUT` | `/api/places/{id}` | Update place |
| `DELETE` | `/api/places/{id}` | Delete place |
| `POST` | `/media` | Upload media file |
| `POST` | `/api/auth/logout` | Logout |

**Admin only:**
| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/auth/organizer-key/generate` | Generate organizer invite |

**Dev profile only (`@Profile("dev")`) - not available in production:**
| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/admin/tools/ingest/{pageId}` | Manually ingest Facebook events for a page |
| `GET` | `/admin/tools/pages` | List all tracked pages with token status |
| `POST` | `/admin/tools/seed` | Seed test data |
| `DELETE` | `/admin/tools/seed` | Clear seeded test data |
| `POST` | `/admin/tools/refresh-tokens` | Refresh tokens for all pages |
| `POST` | `/admin/tools/refresh-tokens/{pageId}` | Refresh token for one page |

## TODO

Backend

- [in progress] JWT auth - signed token, expiry, validation filter
- [in progress] Auto Facebook token refresh
- [ ] Persist likes to backend (`/api/users/me/likes`) - currently localStorage only
- [ ] Fix the damn env situation
- [ ] Replace `ddl-auto: update` with Flyway migrations
- [ ] PicoCLI for proper tool CLI
- [ ] Pin Docker image versions
- [ ] DB: Quartz scheduler
- [ ] Add manual ADMIN endpoint for facebook token refreshing and ingestion

### Frontend

- [in progress] Facebook Page Organizer onboarding flow
- [in progress] Mobile layout improvements
- [ ] Facebook OAuth login
- [ ] Organizer dashboard (event sync status, token expiry)
- [ ] Create Event page
- [ ] Business Manager integration for stable API access
- [ ] Admin Dashboard/Tool for using ADMIN endpoints
