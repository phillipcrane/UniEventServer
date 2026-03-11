# UniEventServer

Migration Strategy Checklist:

Backend:
- [ ] Define endpoints
- [ ] DB Setup (SQL)
- [ ] Media
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
* Input validation
* API should not be public without auth
* Error handling
* CORS configuration
* Missing HTTP methods in controller eg. updateEvent, deleteEvent
*
* Global exception handler for consistent error responses
* Add logging with SLF4J throughout services
* Fix N+1 query problem with JOIN FETCH in repositories
* Implement pagination for large result sets
* Add cascade delete/orphan removal for page deletion
* Validate location coordinates (latitude: -90 to 90, longitude: -180 to 180)
* Replace ddl-auto with database migrations (Flyway/Liquibase)
* Add Spring Boot Actuator for monitoring/metrics
* Add Springdoc/Swagger API documentation
* Remove unused dependency: spring-boot-starter-webflux
* Add @Valid/@NotNull/@NotBlank validation annotations to DTOs
