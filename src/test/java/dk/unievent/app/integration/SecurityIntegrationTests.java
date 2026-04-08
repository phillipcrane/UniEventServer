package dk.unievent.app.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecurityIntegrationTests {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void healthEndpointShouldStayPublic() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url("/actuator/health")))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    @Test
    void nonPublicActuatorEndpointsShouldBeForbidden() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url("/actuator/metrics")))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());
    }

    @Test
    void preflightFromAllowedOriginShouldReturnCorsHeaders() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url("/api/events")))
            .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
            .header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "GET")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("http://localhost:3000", response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
    }

    @Test
    void preflightFromDisallowedOriginShouldBeRejected() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url("/api/events")))
            .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
            .header("Origin", "http://evil.example")
            .header("Access-Control-Request-Method", "GET")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());
        assertTrue(response.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
    }

    @Test
    void preflightWithDisallowedMethodShouldBeRejected() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url("/api/events")))
            .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
            .header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "PATCH")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());
    }
}
