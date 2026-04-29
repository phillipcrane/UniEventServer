package dk.unievent.app.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecurityConfigTests {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final String ACCESS_COOKIE = "auth_access";
    private static final String CSRF_COOKIE = "csrf_token";
    private static final String CSRF_HEADER = "X-CSRF-Token";

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
        AuthSession session = registerAndLoginRegularUser();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/api/auth/organizer-key/generate")))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Cookie", session.cookieHeader())
                .header(CSRF_HEADER, session.csrfToken())
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"organizer@example.com\",\"organizationName\":\"Test Org\"}"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(403, response.statusCode());
    }

    private AuthSession registerAndLoginRegularUser() throws Exception {
        String unique = String.valueOf(System.nanoTime());
        String email = "security-user-" + unique + "@test.com";
        String username = "securityuser" + unique;
        String password = "password123456";

        String registerBody = "{\"username\":\"" + username + "\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        HttpResponse<String> registerResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(url("/api/auth/register")))
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .POST(HttpRequest.BodyPublishers.ofString(registerBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        if (registerResponse.statusCode() == 200) {
            return AuthSession.from(registerResponse);
        }

        String loginBody = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        HttpResponse<String> loginResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(url("/api/auth/login")))
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .POST(HttpRequest.BodyPublishers.ofString(loginBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        return AuthSession.from(loginResponse);
    }

    private record AuthSession(String accessToken, String csrfToken) {
        static AuthSession from(HttpResponse<String> response) {
            return new AuthSession(
                    readCookieValue(response.headers().allValues("Set-Cookie"), ACCESS_COOKIE),
                    readCookieValue(response.headers().allValues("Set-Cookie"), CSRF_COOKIE)
            );
        }

        String cookieHeader() {
            return ACCESS_COOKIE + "=" + accessToken + "; " + CSRF_COOKIE + "=" + csrfToken;
        }

        private static String readCookieValue(List<String> setCookieHeaders, String name) {
            String prefix = name + "=";
            return setCookieHeaders.stream()
                    .filter(header -> header.startsWith(prefix))
                    .findFirst()
                    .map(header -> header.substring(prefix.length(), header.indexOf(';')))
                    .orElseThrow(() -> new AssertionError("Missing Set-Cookie header for " + name));
        }
    }
}
