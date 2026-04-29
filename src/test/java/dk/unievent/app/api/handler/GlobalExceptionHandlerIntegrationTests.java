package dk.unievent.app.api.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(GlobalExceptionHandlerIntegrationTests.TestExceptionController.class)
class GlobalExceptionHandlerIntegrationTests {

    private static final String ACCESS_COOKIE = "auth_access";
    private static final String CSRF_COOKIE = "csrf_token";
    private static final String CSRF_HEADER = "X-CSRF-Token";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${local.server.port}")
    private int port;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private AuthSession getAuthSession() throws Exception {
        String regBody = "{\"username\":\"geh-testuser\",\"email\":\"geh-testuser@test.com\",\"password\":\"password123456\"}";
        HttpResponse<String> regResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(url("/api/auth/register")))
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .POST(HttpRequest.BodyPublishers.ofString(regBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (regResponse.statusCode() == 200) {
            return AuthSession.from(regResponse);
        }
        String loginBody = "{\"email\":\"geh-testuser@test.com\",\"password\":\"password123456\"}";
        HttpResponse<String> loginResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(url("/api/auth/login")))
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .POST(HttpRequest.BodyPublishers.ofString(loginBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        return AuthSession.from(loginResponse);
    }

    @Test
    void validationErrorShouldReturnStructuredErrorResponse() throws Exception {
        AuthSession session = getAuthSession();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/api/events")))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Cookie", session.cookieHeader())
                .header(CSRF_HEADER, session.csrfToken())
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});

        assertEquals(400, response.statusCode());
        assertEquals(400, ((Number) body.get("status")).intValue());
        assertEquals("Validation Failed", body.get("error"));
        assertNotNull(body.get("message"));
        assertTrue(body.containsKey("errors"));
    }

    @Test
    void malformedJsonShouldReturnBadRequestShape() throws Exception {
        AuthSession session = getAuthSession();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/api/events")))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Cookie", session.cookieHeader())
                .header(CSRF_HEADER, session.csrfToken())
                .POST(HttpRequest.BodyPublishers.ofString("{\"title\":\"broken\""))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});

        assertEquals(400, response.statusCode());
        assertEquals(400, ((Number) body.get("status")).intValue());
        assertEquals("Bad Request", body.get("error"));
        assertTrue(body.containsKey("timestamp"));
        assertTrue(body.containsKey("message"));
    }

    @Test
    void missingRequestParameterShouldReturnBadRequestShape() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/api/pages/search")))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});

        assertEquals(400, response.statusCode());
        assertEquals(400, ((Number) body.get("status")).intValue());
        assertEquals("Bad Request", body.get("error"));
        assertTrue(body.containsKey("timestamp"));
        assertTrue(body.containsKey("message"));
    }

    @Test
    void typeMismatchShouldBeHandledAsBadRequest() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/media/not-a-number")))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});

        assertEquals(400, response.statusCode());
        assertEquals(400, ((Number) body.get("status")).intValue());
        assertEquals("Bad Request", body.get("error"));
    }

    @Test
    void uncaughtRuntimeExceptionShouldUseGlobalFallback() throws Exception {
        AuthSession session = getAuthSession();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/test-exceptions/runtime-error")))
                .header("Cookie", session.cookieHeader())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});

        assertEquals(500, response.statusCode());
        assertEquals(500, ((Number) body.get("status")).intValue());
        assertEquals("Internal Server Error", body.get("error"));
        assertEquals("An unexpected error occurred.", body.get("message"));
    }

    @Test
    void maxUploadExceptionShouldReturnPayloadTooLargeShape() throws Exception {
        AuthSession session = getAuthSession();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/test-exceptions/max-upload")))
                .header("Cookie", session.cookieHeader())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});

        assertEquals(413, response.statusCode());
        assertEquals(413, ((Number) body.get("status")).intValue());
        assertEquals("Payload Too Large", body.get("error"));
        assertTrue(body.containsKey("timestamp"));
        assertTrue(body.containsKey("message"));
    }

    @RestController
    @RequestMapping("/test-exceptions")
    static class TestExceptionController {
        @GetMapping("/max-upload")
        String maxUpload() {
            throw new MaxUploadSizeExceededException(1L);
        }

        @GetMapping("/runtime-error")
        String runtimeError() {
            throw new RuntimeException("unexpected failure");
        }
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
