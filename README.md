# UniEventServer

Migration Strategy Checklist:

Backend:
- [x] Define endpoints
- [x] DB Setup (SQL)
- [ ] Media (filesystem storage with DB metadata)
- [ ] Serverless Funtions
- [x] Secret Manager
- [x] Hosting / Debian Server

Frontend:
- [x] Security update
- [ ] New DAL
- [ ] Auth/User function
- [x] Calendar function
- [ ] Login function

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
