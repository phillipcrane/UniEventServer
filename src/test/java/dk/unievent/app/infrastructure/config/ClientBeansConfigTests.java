package dk.unievent.app.infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ClientBeansConfigTests {

    @Test
    void shouldCreateRestTemplateAndObjectMapperBeans() {
        ClientBeansConfig config = new ClientBeansConfig();

        assertNotNull(config.restTemplate());
        assertNotNull(config.objectMapper());
    }
}
