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
import dk.unievent.app.db.repository.RefreshTokenRepository;
import dk.unievent.app.db.repository.UserEventLikeRepository;
import dk.unievent.app.db.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = WebApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.aop.auto=false")
class UserLikesHttpIntegrationTests {

    private static final String ACCESS_COOKIE = "auth_access";
    private static final String CSRF_COOKIE = "csrf_token";
    private static final String CSRF_HEADER = "X-CSRF-Token";

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

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserEventLikeRepository userEventLikeRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void seedEvent() {
        cleanDatabase();
        PageEntity page = pageRepository.save(PageEntity.builder()
            .id("likes-flow-page")
            .name("Likes Flow Page")
            .tokenStatus("valid")
            .build());
        PlaceEntity place = placeRepository.save(PlaceEntity.builder()
            .id("likes-flow-place")
            .name("Likes Flow Hall")
            .city("Kongens Lyngby")
            .country("Denmark")
            .build());
        eventRepository.save(EventEntity.builder()
            .id("likes-flow-event")
            .title("Likes Flow Event")
            .description("Liked through an authenticated HTTP flow")
            .startTime(LocalDateTime.now().plusDays(3))
            .page(page)
            .place(place)
            .build());
    }

    @AfterEach
    void cleanDatabase() {
        userEventLikeRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        eventRepository.deleteAll();
        pageRepository.deleteAll();
        placeRepository.deleteAll();
    }

    @Test
    void signupLikeAndProfileLikesShouldWorkThroughHttp() throws Exception {
        AuthSession session = registerSession();

        HttpResponse<String> profileResponse = send(HttpRequest.newBuilder()
            .uri(URI.create(url("/api/auth/profile")))
            .header("Cookie", session.cookieHeader())
            .GET()
            .build());
        assertEquals(200, profileResponse.statusCode());
        assertEquals("ROLE_USER", objectMapper.readTree(profileResponse.body()).path("role").asText());

        HttpResponse<String> likeResponse = send(HttpRequest.newBuilder()
            .uri(URI.create(url("/api/users/me/likes/likes-flow-event")))
            .header("Cookie", session.cookieHeader())
            .header(CSRF_HEADER, session.csrfToken())
            .PUT(HttpRequest.BodyPublishers.noBody())
            .build());
        assertEquals(200, likeResponse.statusCode());
        assertEventIds(likeResponse.body(), List.of("likes-flow-event"));

        HttpResponse<String> getLikesResponse = send(HttpRequest.newBuilder()
            .uri(URI.create(url("/api/users/me/likes")))
            .header("Cookie", session.cookieHeader())
            .GET()
            .build());
        assertEquals(200, getLikesResponse.statusCode());
        assertEventIds(getLikesResponse.body(), List.of("likes-flow-event"));
    }

    @Test
    void authenticatedLikeShouldRequireValidCsrfToken() throws Exception {
        AuthSession session = registerSession();

        HttpResponse<String> response = send(HttpRequest.newBuilder()
            .uri(URI.create(url("/api/users/me/likes/likes-flow-event")))
            .header("Cookie", session.cookieHeader())
            .PUT(HttpRequest.BodyPublishers.noBody())
            .build());

        assertEquals(403, response.statusCode());
    }

    private AuthSession registerSession() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String body = """
            {"username":"likes%s","email":"likes%s@example.com","password":"secret12345678"}
            """.formatted(suffix.substring(0, 12), suffix);
        HttpResponse<String> response = send(HttpRequest.newBuilder()
            .uri(URI.create(url("/api/auth/register")))
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build());

        assertEquals(200, response.statusCode());
        return AuthSession.from(response);
    }

    private void assertEventIds(String responseBody, List<String> expectedIds) throws Exception {
        JsonNode eventIds = objectMapper.readTree(responseBody).path("eventIds");

        assertEquals(expectedIds.size(), eventIds.size());
        for (int i = 0; i < expectedIds.size(); i++) {
            assertEquals(expectedIds.get(i), eventIds.get(i).asText());
        }
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
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
