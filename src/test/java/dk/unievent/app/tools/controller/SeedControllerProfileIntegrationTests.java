package dk.unievent.app.tools.controller;

import dk.unievent.app.api.controller.AdminIngestController;
import dk.unievent.app.api.controller.AdminPagesController;
import dk.unievent.app.api.controller.AdminTokenRefreshController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SeedControllerProfileIntegrationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void seedControllerShouldNotBeLoadedOutsideDevProfile() {
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(SeedController.class));
    }

    @Test
    void productionAdminFacebookControllersShouldBeLoadedOutsideDevProfile() {
        assertNotNull(context.getBean(AdminIngestController.class));
        assertNotNull(context.getBean(AdminPagesController.class));
        assertNotNull(context.getBean(AdminTokenRefreshController.class));
    }
}
