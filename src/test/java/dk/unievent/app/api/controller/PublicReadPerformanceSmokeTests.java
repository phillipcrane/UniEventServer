package dk.unievent.app.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.unievent.app.WebApplication;
import dk.unievent.app.db.model.EventEntity;
import dk.unievent.app.db.model.PageEntity;
import dk.unievent.app.db.model.PlaceEntity;
import dk.unievent.app.db.repository.EventRepository;
import dk.unievent.app.db.repository.PageRepository;
import dk.unievent.app.db.repository.PlaceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = WebApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.aop.auto=false")
class PublicReadPerformanceSmokeTests {

    private static final Duration PROMPT_RESPONSE_BUDGET = Duration.ofSeconds(5);

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @BeforeEach
    void seedReadableData() {
        cleanDatabase();

        PageEntity page = pageRepository.save(PageEntity.builder()
            .id("perf-page-dtu")
            .name("DTU Performance Page")
            .tokenStatus("valid")
            .build());
        PlaceEntity place = placeRepository.save(PlaceEntity.builder()
            .id("perf-place-lyngby")
            .name("DTU Lyngby Hall")
            .city("Kongens Lyngby")
            .country("Denmark")
            .build());

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        for (int i = 0; i < 120; i++) {
            eventRepository.save(EventEntity.builder()
                .id("perf-event-" + i)
                .title("Performance Event " + i)
                .description("Seeded performance smoke event")
                .startTime(start.plusHours(i))
                .endTime(start.plusHours(i + 2))
                .page(page)
                .place(place)
                .build());
        }
    }

    @AfterEach
    void cleanDatabase() {
        eventRepository.deleteAll();
        pageRepository.deleteAll();
        placeRepository.deleteAll();
    }

    @Test
    void publicReadEndpointsShouldReturnPromptlyWithSeededData() throws Exception {
        assertPagedEndpoint("/api/events?size=20", 120);
        assertPagedEndpoint("/api/events/future?size=20", 120);
        assertPagedEndpoint("/api/pages/search?name=Performance&size=20", 1);
        assertPagedEndpoint("/api/places/search?name=Lyngby&size=20", 1);
    }

    @Test
    void healthEndpointShouldReturnPromptly() throws Exception {
        TimedResponse response = get("/actuator/health");

        assertEquals(200, response.statusCode());
        assertTrue(response.elapsed().compareTo(PROMPT_RESPONSE_BUDGET) < 0,
            "Expected /actuator/health to respond within " + PROMPT_RESPONSE_BUDGET);
    }

    private void assertPagedEndpoint(String path, int expectedTotalElements) throws Exception {
        TimedResponse response = get(path);

        assertEquals(200, response.statusCode(), path);
        assertTrue(response.elapsed().compareTo(PROMPT_RESPONSE_BUDGET) < 0,
            "Expected " + path + " to respond within " + PROMPT_RESPONSE_BUDGET);
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(expectedTotalElements, totalElements(body), path);
    }

    private int totalElements(JsonNode pageBody) {
        if (pageBody.has("totalElements")) {
            return pageBody.path("totalElements").asInt();
        }
        return pageBody.path("page").path("totalElements").asInt();
    }

    private TimedResponse get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + path))
            .timeout(PROMPT_RESPONSE_BUDGET)
            .GET()
            .build();
        long started = System.nanoTime();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Duration elapsed = Duration.ofNanos(System.nanoTime() - started);

        return new TimedResponse(response.statusCode(), response.body(), elapsed);
    }

    private record TimedResponse(int statusCode, String body, Duration elapsed) {
    }
}
