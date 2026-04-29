package dk.unievent.app.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CorsConfigTests {

    @Test
    void shouldExposeConfiguredCorsValuesForApiPath() {
        CorsConfig config = new CorsConfig();
        config.setAllowedOrigins(List.of("https://unievent.dk"));
        config.setAllowedMethods(List.of("GET", "POST"));
        config.setAllowedHeaders(List.of("Content-Type", "X-CSRF-Token"));
        config.setAllowCredentials(false);
        config.setMaxAge(99L);

        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/events"));

        assertNotNull(cors);
        assertEquals(List.of("https://unievent.dk"), cors.getAllowedOrigins());
        assertEquals(List.of("GET", "POST"), cors.getAllowedMethods());
        assertEquals(List.of("Content-Type", "X-CSRF-Token"), cors.getAllowedHeaders());
        assertEquals(false, cors.getAllowCredentials());
        assertEquals(99L, cors.getMaxAge());
    }
}
