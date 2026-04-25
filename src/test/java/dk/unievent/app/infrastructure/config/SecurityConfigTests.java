package dk.unievent.app.infrastructure.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecurityConfigTests {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${local.server.port}")
    private int port;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void healthAndInfoShouldBePubliclyAccessible() throws Exception {
        HttpRequest health = HttpRequest.newBuilder().uri(URI.create(url("/actuator/health"))).GET().build();
        HttpRequest info = HttpRequest.newBuilder().uri(URI.create(url("/actuator/info"))).GET().build();

        HttpResponse<String> healthResponse = httpClient.send(health, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> infoResponse = httpClient.send(info, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, healthResponse.statusCode());
        assertEquals(200, infoResponse.statusCode());
    }

    @Test
    void otherActuatorEndpointsShouldBeDenied() throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url("/actuator/metrics"))).GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(403, response.statusCode());
    }

    @Test
    void organizerKeyGenerateShouldRequireAuthentication() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/api/auth/organizer-key/generate")))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"organizer@example.com\",\"organizationName\":\"Test Org\"}"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(403, response.statusCode());
    }

    @Test
    void organizerKeyGenerateShouldRejectNonAdminUsers() throws Exception {
        String token = registerAndLoginRegularUser();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/api/auth/organizer-key/generate")))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"organizer@example.com\",\"organizationName\":\"Test Org\"}"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(403, response.statusCode());
    }

    private String registerAndLoginRegularUser() throws Exception {
        String unique = String.valueOf(System.nanoTime());
        String email = "security-user-" + unique + "@test.com";
        String username = "securityuser" + unique;
        String password = "password1234";

        String registerBody = "{\"username\":\"" + username + "\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        HttpResponse<String> registerResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(url("/api/auth/register")))
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .POST(HttpRequest.BodyPublishers.ofString(registerBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        if (registerResponse.statusCode() == 200) {
            Map<String, Object> payload = objectMapper.readValue(registerResponse.body(), new TypeReference<>() {});
            return (String) payload.get("token");
        }

        String loginBody = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        HttpResponse<String> loginResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(url("/api/auth/login")))
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .POST(HttpRequest.BodyPublishers.ofString(loginBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        Map<String, Object> loginPayload = objectMapper.readValue(loginResponse.body(), new TypeReference<>() {});
        return (String) loginPayload.get("token");
    }
}
