package dk.unievent.app.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${local.server.port}")
    private int port;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void validationErrorShouldReturnStructuredErrorResponse() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/api/events")))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
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
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/api/events")))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
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
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/media/99999999")))
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
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/test-exceptions/max-upload")))
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
    }
}
