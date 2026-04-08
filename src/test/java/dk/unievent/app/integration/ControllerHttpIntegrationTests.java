package dk.unievent.app.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.unievent.app.db.repository.EventRepository;
import dk.unievent.app.db.repository.PageRepository;
import dk.unievent.app.db.repository.PlaceRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ControllerHttpIntegrationTests {

        @Autowired
        private EventRepository eventRepository;

        @Autowired
        private PageRepository pageRepository;

        @Autowired
        private PlaceRepository placeRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${local.server.port}")
    private int port;

        @BeforeEach
        void cleanDatabase() {
                eventRepository.deleteAll();
                pageRepository.deleteAll();
                placeRepository.deleteAll();
        }

        @AfterEach
        void cleanDatabaseAfter() {
                cleanDatabase();
        }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void eventCreateAndGetShouldWorkThroughHttp() throws Exception {
        String pageId = "page-http-" + UUID.randomUUID();
        String eventId = "event-http-" + UUID.randomUUID();

        String pageBody = "{\"id\":\"" + pageId + "\",\"name\":\"HTTP Page\"}";
        HttpRequest createPage = HttpRequest.newBuilder()
                .uri(URI.create(url("/api/pages")))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(pageBody))
                .build();
        HttpResponse<String> pageResponse = httpClient.send(createPage, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, pageResponse.statusCode());

        String eventBody = "{" +
                "\"id\":\"" + eventId + "\"," +
                "\"pageId\":\"" + pageId + "\"," +
                "\"title\":\"HTTP Event\"," +
                "\"description\":\"Created through API\"," +
                "\"startTime\":\"2030-01-10T18:00:00\"," +
                "\"endTime\":\"2030-01-10T20:00:00\"" +
                "}";

        HttpRequest createEvent = HttpRequest.newBuilder()
                .uri(URI.create(url("/api/events")))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(eventBody))
                .build();

        HttpResponse<String> createEventResponse = httpClient.send(createEvent, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, createEventResponse.statusCode());

        Map<String, Object> created = objectMapper.readValue(createEventResponse.body(), new TypeReference<Map<String, Object>>() {});
        assertEquals(eventId, created.get("id"));
        assertEquals("HTTP Event", created.get("title"));

        HttpRequest getEvent = HttpRequest.newBuilder()
                .uri(URI.create(url("/api/events/" + eventId)))
                .GET()
                .build();

        HttpResponse<String> getEventResponse = httpClient.send(getEvent, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getEventResponse.statusCode());

        Map<String, Object> fetched = objectMapper.readValue(getEventResponse.body(), new TypeReference<Map<String, Object>>() {});
        assertEquals(eventId, fetched.get("id"));
        assertEquals("HTTP Event", fetched.get("title"));
    }

    @Test
    void eventNotFoundShouldReturn404() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/api/events/non-existent-event")))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

    @Test
    void pageValidationErrorShouldReturn400() throws Exception {
        String body = "{\"id\":\"bad-page\",\"name\":\"\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/api/pages")))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());

        Map<String, Object> error = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
        assertEquals(400, ((Number) error.get("status")).intValue());
        assertTrue(error.containsKey("errors"));
    }

    @Test
    void placeCreateAndGetShouldWorkThroughHttp() throws Exception {
        String placeId = "place-http-" + UUID.randomUUID();

        String createBody = "{" +
                "\"id\":\"" + placeId + "\"," +
                "\"name\":\"HTTP Venue\"," +
                "\"location\":{" +
                "\"street\":\"Street 1\"," +
                "\"city\":\"Copenhagen\"," +
                "\"zip\":\"2100\"," +
                "\"country\":\"Denmark\"," +
                "\"latitude\":55.67," +
                "\"longitude\":12.58" +
                "}" +
                "}";

        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(url("/api/places")))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(createBody))
                .build();

        HttpResponse<String> createResponse = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, createResponse.statusCode());

        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(url("/api/places/" + placeId)))
                .GET()
                .build();

        HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getResponse.statusCode());

        Map<String, Object> body = objectMapper.readValue(getResponse.body(), new TypeReference<Map<String, Object>>() {});
        assertEquals(placeId, body.get("id"));
        assertEquals("HTTP Venue", body.get("name"));
        assertNotNull(body.get("location"));
    }

    @Test
    void placeUpdateNotFoundShouldReturn404() throws Exception {
        String body = "{" +
                "\"name\":\"Missing Venue\"," +
                "\"location\":{" +
                "\"street\":\"Street 2\"," +
                "\"city\":\"Aarhus\"," +
                "\"zip\":\"8000\"," +
                "\"country\":\"Denmark\"," +
                "\"latitude\":56.15," +
                "\"longitude\":10.20" +
                "}" +
                "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/api/places/not-found-place")))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }
}
