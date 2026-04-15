package dk.unievent.app.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${local.server.port}")
    private int port;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void healthEndpointShouldRequireAuthentication() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url("/actuator/health")))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());
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

    @Test
    void protectedAdminEndpointShouldRejectAnonymousRequests() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url("/admin/seed")))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());
    }

    @Test
    void jwtFromRegisterShouldAuthenticateProtectedEndpoints() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String registerJson = "{\"username\":\"user" + suffix + "\",\"email\":\"user" + suffix + "@example.com\",\"password\":\"secret123\"}";

        HttpRequest registerRequest = HttpRequest.newBuilder()
            .uri(URI.create(url("/api/auth/register")))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(registerJson))
            .build();

        HttpResponse<String> registerResponse = httpClient.send(registerRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, registerResponse.statusCode());

        JsonNode payload = objectMapper.readTree(registerResponse.body());
        String token = payload.path("token").asText();

        HttpRequest healthRequest = HttpRequest.newBuilder()
            .uri(URI.create(url("/actuator/health")))
            .header("Authorization", "Bearer " + token)
            .GET()
            .build();

        HttpResponse<String> healthResponse = httpClient.send(healthRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, healthResponse.statusCode());
    }

    @Test
    void refreshTokenShouldRotateAndLogoutShouldRevokeIt() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String registerJson = "{\"username\":\"refresh" + suffix + "\",\"email\":\"refresh" + suffix + "@example.com\",\"password\":\"secret123\"}";

        HttpRequest registerRequest = HttpRequest.newBuilder()
            .uri(URI.create(url("/api/auth/register")))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(registerJson))
            .build();

        HttpResponse<String> registerResponse = httpClient.send(registerRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, registerResponse.statusCode());

        JsonNode registerPayload = objectMapper.readTree(registerResponse.body());
        String accessToken = registerPayload.path("token").asText();
        String refreshToken = registerPayload.path("refreshToken").asText();

        assertTrue(!accessToken.isBlank());

        HttpRequest refreshRequest = HttpRequest.newBuilder()
            .uri(URI.create(url("/api/auth/refresh")))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"refreshToken\":\"" + refreshToken + "\"}"))
            .build();

        HttpResponse<String> refreshResponse = httpClient.send(refreshRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, refreshResponse.statusCode());

        JsonNode refreshPayload = objectMapper.readTree(refreshResponse.body());
        String rotatedAccessToken = refreshPayload.path("token").asText();
        String rotatedRefreshToken = refreshPayload.path("refreshToken").asText();

        assertTrue(!rotatedAccessToken.isBlank());
        assertTrue(!rotatedRefreshToken.isBlank());

        HttpRequest logoutRequest = HttpRequest.newBuilder()
            .uri(URI.create(url("/api/auth/logout")))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"refreshToken\":\"" + rotatedRefreshToken + "\"}"))
            .build();

        HttpResponse<String> logoutResponse = httpClient.send(logoutRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(204, logoutResponse.statusCode());

        HttpResponse<String> postLogoutRefreshResponse = httpClient.send(refreshRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, postLogoutRefreshResponse.statusCode());
    }
}
