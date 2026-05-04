package dk.unievent.app.infrastructure.config;

import dk.unievent.app.WebApplication;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = WebApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = FlywayMigrationIntegrationTests.MigrateSchemaInitializer.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:flywayvalidation;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=false"
})
class FlywayMigrationIntegrationTests {

    @Test
    void flywayMigrationsShouldCreateSchemaValidatedByHibernate() {
        // Context startup validates the Flyway-created schema against the JPA model.
    }

    static class MigrateSchemaInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            Flyway.configure()
                    .dataSource(
                            "jdbc:h2:mem:flywayvalidation;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
                            "sa",
                            ""
                    )
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(false)
                    .load()
                    .migrate();
        }
    }
}
