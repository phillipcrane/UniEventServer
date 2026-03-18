# UniEventServer

Migration Strategy Checklist:

Backend:
- [x] Define endpoints
- [x] DB Setup (SQL)
- [x] Media (filesystem storage with DB metadata)

## Media handling

The server stores uploaded files on disk (or in a mounted volume) and keeps a reference in MySQL.  The following REST endpoints are available:

- `POST /media` – multipart upload parameter `file`. Returns JSON metadata including `id`.
- `GET /media/{id}` – download the file as an attachment.
- `GET /media` – list all media records.

Docker compose now mounts a `media-data` volume at `/app/media` inside the container.  The storage location is configurable via `unievent.media.location` (environment variable `UNIEVENT_MEDIA_LOCATION`).
- [ ] Serverless Funtions
- [ ] Secret Manager
- [ ] Hosting / Debian Server

Frontend:
- [ ] Security update
- [ ] New DAL
- [ ] Auth/User function
- [ ] Calendar function

##

TODO Database
* Input validation // DONE
* API should not be public without auth
* Error handling // Partial (Validation error handling is implemented)
* CORS configuration
* Missing HTTP methods in controller eg. updateEvent, deleteEvent
*
* Global exception handler for consistent error responses (Done)
* Add logging with SLF4J throughout services
* Fix N+1 query problem with JOIN FETCH in repositories
* Implement pagination for large result sets
* Add cascade delete/orphan removal for page deletion
* Validate location coordinates (latitude: -90 to 90, longitude: -180 to 180) // DONE
* Replace ddl-auto with database migrations (Flyway/Liquibase)
* Add Spring Boot Actuator for monitoring/metrics // DONE (HEALTHCHECK NOT SET UP)
* Add Springdoc/Swagger API documentation // DONE
* Remove unused dependency: spring-boot-starter-webflux // DONE
* Add @Valid/@NotNull/@NotBlank validation annotations to DTOs // DONE
