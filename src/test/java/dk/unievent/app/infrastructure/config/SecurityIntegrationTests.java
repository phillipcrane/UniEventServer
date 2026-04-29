package dk.unievent.app.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.unievent.app.WebApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = WebApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.aop.auto=false")
class SecurityIntegrationTests {

    private static final String ACCESS_COOKIE = "auth_access";
    private static final String REFRESH_COOKIE = "auth_refresh";
    private static final String CSRF_COOKIE = "csrf_token";
    private static final String CSRF_HEADER = "X-CSRF-Token";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${local.server.port}")
    private int port;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void healthEndpointShouldBePubliclyAccessible() throws Exception {
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
    void cookiesFromRegisterShouldAuthenticateProtectedEndpoints() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        AuthSession session = registerSession("user" + suffix, "user" + suffix + "@example.com");

        // POST /api/events with valid auth cookies + CSRF but empty body - expect 400 (validation), not 403 (auth failure)
        HttpRequest writeRequest = HttpRequest.newBuilder()
            .uri(URI.create(url("/api/events")))
            .header("Content-Type", "application/json")
            .header("Cookie", session.cookieHeader(ACCESS_COOKIE, CSRF_COOKIE))
            .header(CSRF_HEADER, session.csrfToken())
            .POST(HttpRequest.BodyPublishers.ofString("{}"))
            .build();

        HttpResponse<String> writeResponse = httpClient.send(writeRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, writeResponse.statusCode());
    }

    @Test
    void unauthenticatedPostToApiShouldBeForbidden() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url("/api/events")))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{}"))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());
    }

    @Test
    void unauthenticatedPutToApiShouldBeForbidden() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url("/api/events/any-id")))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString("{}"))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());
    }

    @Test
    void unauthenticatedDeleteToApiShouldBeForbidden() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url("/api/events/any-id")))
            .DELETE()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());
    }

    @Test
    void refreshTokenShouldRotateAndLogoutShouldRevokeIt() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        AuthSession session = registerSession("refresh" + suffix, "refresh" + suffix + "@example.com");
        assertTrue(!session.accessToken().isBlank());

        HttpRequest refreshRequest = HttpRequest.newBuilder()
            .uri(URI.create(url("/api/auth/refresh")))
            .header("Content-Type", "application/json")
            .header("Cookie", session.cookieHeader(REFRESH_COOKIE, CSRF_COOKIE))
            .header(CSRF_HEADER, session.csrfToken())
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> refreshResponse = httpClient.send(refreshRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, refreshResponse.statusCode());

        JsonNode refreshPayload = objectMapper.readTree(refreshResponse.body());
        AuthSession rotated = AuthSession.from(refreshResponse);

        assertTrue(!rotated.accessToken().isBlank());
        assertTrue(!rotated.refreshToken().isBlank());
        assertTrue(!refreshPayload.path("csrfToken").asText().isBlank());

        HttpRequest logoutRequest = HttpRequest.newBuilder()
            .uri(URI.create(url("/api/auth/logout")))
            .header("Content-Type", "application/json")
            .header("Cookie", rotated.cookieHeader(REFRESH_COOKIE, CSRF_COOKIE))
            .header(CSRF_HEADER, rotated.csrfToken())
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> logoutResponse = httpClient.send(logoutRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(204, logoutResponse.statusCode());

        HttpRequest postLogoutRefreshRequest = HttpRequest.newBuilder()
            .uri(URI.create(url("/api/auth/refresh")))
            .header("Content-Type", "application/json")
            .header("Cookie", rotated.cookieHeader(REFRESH_COOKIE, CSRF_COOKIE))
            .header(CSRF_HEADER, rotated.csrfToken())
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        HttpResponse<String> postLogoutRefreshResponse = httpClient.send(postLogoutRefreshRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, postLogoutRefreshResponse.statusCode());
    }

    private AuthSession registerSession(String username, String email) throws Exception {
        String registerJson = "{\"username\":\"" + username + "\",\"email\":\"" + email + "\",\"password\":\"secret12345678\"}";

        HttpRequest registerRequest = HttpRequest.newBuilder()
            .uri(URI.create(url("/api/auth/register")))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(registerJson))
            .build();

        HttpResponse<String> registerResponse = httpClient.send(registerRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, registerResponse.statusCode());

        JsonNode payload = objectMapper.readTree(registerResponse.body());
        assertTrue(!payload.path("csrfToken").asText().isBlank());
        return AuthSession.from(registerResponse);
    }

    private record AuthSession(String accessToken, String refreshToken, String csrfToken) {
        static AuthSession from(HttpResponse<String> response) {
            return new AuthSession(
                readCookieValue(response.headers().allValues("Set-Cookie"), ACCESS_COOKIE),
                readCookieValue(response.headers().allValues("Set-Cookie"), REFRESH_COOKIE),
                readCookieValue(response.headers().allValues("Set-Cookie"), CSRF_COOKIE)
            );
        }

        String cookieHeader(String... names) {
            StringBuilder header = new StringBuilder();
            for (String name : names) {
                if (!header.isEmpty()) {
                    header.append("; ");
                }
                header.append(name).append("=").append(valueFor(name));
            }
            return header.toString();
        }

        private String valueFor(String name) {
            return switch (name) {
                case ACCESS_COOKIE -> accessToken;
                case REFRESH_COOKIE -> refreshToken;
                case CSRF_COOKIE -> csrfToken;
                default -> throw new IllegalArgumentException("Unknown cookie: " + name);
            };
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
