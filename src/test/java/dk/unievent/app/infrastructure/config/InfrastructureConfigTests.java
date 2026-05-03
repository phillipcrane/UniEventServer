package dk.unievent.app.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfigurationSource;

import io.swagger.v3.oas.models.OpenAPI;

class InfrastructureConfigTests {

    @Test
    void infrastructureConfigsSmokeTest() {
        CorsConfig corsConfig = new CorsConfig(new MockEnvironment());
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();

        MockHttpServletRequest apiRequest = new MockHttpServletRequest("OPTIONS", "/api/events");
        var apiCors = source.getCorsConfiguration(apiRequest);
        assertNotNull(apiCors);

        MockHttpServletRequest mediaRequest = new MockHttpServletRequest("OPTIONS", "/media/123");
        var mediaCors = source.getCorsConfiguration(mediaRequest);
        assertNotNull(mediaCors);

        OpenApiConfig openApiConfig = new OpenApiConfig();
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertEquals("UniEvent API", openAPI.getInfo().getTitle());
    }
}
