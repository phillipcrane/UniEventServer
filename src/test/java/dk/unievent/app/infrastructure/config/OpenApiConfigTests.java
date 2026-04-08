package dk.unievent.app.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenApiConfigTests {

    @Test
    void shouldBuildOpenApiInfoFromConfiguredProperties() {
        OpenApiConfig config = new OpenApiConfig();
        config.setTitle("UniEvent Test API");
        config.setVersion("2.0.0");
        config.setDescription("Test description");
        config.setContactName("Support");
        config.setContactEmail("support@example.com");
        config.setLicenseName("MIT");
        config.setLicenseUrl("https://opensource.org/licenses/MIT");

        OpenAPI api = config.customOpenAPI();

        assertEquals("UniEvent Test API", api.getInfo().getTitle());
        assertEquals("2.0.0", api.getInfo().getVersion());
        assertEquals("Test description", api.getInfo().getDescription());
        assertEquals("Support", api.getInfo().getContact().getName());
        assertEquals("support@example.com", api.getInfo().getContact().getEmail());
        assertEquals("MIT", api.getInfo().getLicense().getName());
        assertEquals("https://opensource.org/licenses/MIT", api.getInfo().getLicense().getUrl());
    }
}
