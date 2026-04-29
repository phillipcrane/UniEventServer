package dk.unievent.app.api.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.unievent.app.db.model.MediaEntity;
import dk.unievent.app.db.repository.MediaRepository;
import dk.unievent.app.infrastructure.client.SeaweedFsClient;
import dk.unievent.app.infrastructure.config.SeaweedConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(MediaControllerHttpIntegrationTests.MediaTestConfig.class)
class MediaControllerHttpIntegrationTests {

    private static final String ACCESS_COOKIE = "auth_access";
    private static final String CSRF_COOKIE = "csrf_token";
    private static final String CSRF_HEADER = "X-CSRF-Token";

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private SeaweedFsClient seaweedFsClient;

    @Value("${local.server.port}")
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private FakeSeaweedFsClient fakeSeaweed;

    @BeforeEach
    void setUp() {
        mediaRepository.deleteAll();
        fakeSeaweed = (FakeSeaweedFsClient) seaweedFsClient;
        fakeSeaweed.reset();
    }

    @Test
    void uploadShouldReturnDtoAndPersistMetadata() throws Exception {
        AuthSession session = getAuthSession();
        String boundary = "----unievent-boundary";
        byte[] pngMagic = {(byte)0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};
        byte[] body = multipartBody(boundary, "file", "poster.png", "image/png", pngMagic);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/media")))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Cookie", session.cookieHeader())
                .header(CSRF_HEADER, session.csrfToken())
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});

        assertEquals(200, response.statusCode());
        assertEquals("poster.png", payload.get("filename"));
        assertEquals("image/png", payload.get("contentType"));
        assertEquals("1,test", payload.get("fileId"));
        assertTrue(payload.containsKey("id"));

        List<MediaEntity> all = mediaRepository.findAll();
        assertEquals(1, all.size());
        assertEquals("poster.png", all.get(0).getFilename());
    }

    @Test
    void uploadStorageFailureShouldReturnGlobal500Shape() throws Exception {
        AuthSession session = getAuthSession();
        fakeSeaweed.failAssign = true;

        String boundary = "----unievent-boundary";
        byte[] pngMagic = {(byte)0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};
        byte[] body = multipartBody(boundary, "file", "poster.png", "image/png", pngMagic);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/media")))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Cookie", session.cookieHeader())
                .header(CSRF_HEADER, session.csrfToken())
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> error = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});

        assertEquals(500, response.statusCode());
        assertEquals("Internal Server Error", error.get("error"));
        assertEquals(500, ((Number) error.get("status")).intValue());
    }

    @Test
    void listShouldReturnDtos() throws Exception {
        mediaRepository.save(MediaEntity.builder()
                .filename("a.jpg")
                .contentType("image/jpeg")
                .fileId("3,abc")
                .uploadedAt(Instant.now())
                .build());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/media")))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> page = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) page.get("content");

        assertEquals(200, response.statusCode());
        assertFalse(items.isEmpty());
        assertEquals("a.jpg", items.get(0).get("filename"));
        assertEquals("3,abc", items.get(0).get("fileId"));
    }

    @Test
    void downloadShouldReturnBytesForExistingMedia() throws Exception {
        MediaEntity media = mediaRepository.save(MediaEntity.builder()
                .filename("manual.txt")
                .contentType("text/plain")
                .fileId("1,test")
                .uploadedAt(Instant.now())
                .build());

        fakeSeaweed.downloadPayload = "hello-download".getBytes();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/media/" + media.getId())))
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        assertEquals(200, response.statusCode());
        assertArrayEquals("hello-download".getBytes(), response.body());
    }

    @Test
    void downloadMissingMediaShouldReturn404() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/media/999999")))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> error = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});

        assertEquals(404, response.statusCode());
        assertEquals("Not Found", error.get("error"));
        assertEquals(404, ((Number) error.get("status")).intValue());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private AuthSession getAuthSession() throws Exception {
        String regBody = "{\"username\":\"media-testuser\",\"email\":\"media-testuser@test.com\",\"password\":\"password123456\"}";
        HttpResponse<String> regResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(url("/api/auth/register")))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(regBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (regResponse.statusCode() == 200) {
            return AuthSession.from(regResponse);
        }
        String loginBody = "{\"email\":\"media-testuser@test.com\",\"password\":\"password123456\"}";
        HttpResponse<String> loginResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(url("/api/auth/login")))
                        .header("Content-Type", "application/json")
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

    private byte[] multipartBody(String boundary, String fieldName, String filename, String contentType, byte[] fileBytes)
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + filename + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(fileBytes);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    @TestConfiguration
    static class MediaTestConfig {
        @Bean
        @Primary
        SeaweedFsClient seaweedFsClient() {
            return new FakeSeaweedFsClient();
        }
    }

    static class FakeSeaweedFsClient extends SeaweedFsClient {
        boolean failAssign;
        byte[] downloadPayload = "default".getBytes();

        FakeSeaweedFsClient() {
            super(config(), new ObjectMapper());
        }

        private static SeaweedConfig config() {
            SeaweedConfig config = new SeaweedConfig();
            config.setMasterUrl("localhost:9333");
            return config;
        }

        void reset() {
            failAssign = false;
            downloadPayload = "default".getBytes();
        }

        @Override
        public FileAssignment assignFile() throws IOException {
            if (failAssign) {
                throw new IOException("forced assign failure");
            }
            return new FileAssignment("1,test", "localhost:8080");
        }

        @Override
        public void uploadFile(String publicUrl, String fid, String filename, byte[] bytes) {
            // no-op for tests
        }

        @Override
        public byte[] downloadFile(String fileId) {
            return downloadPayload;
        }

        @Override
        public void deleteFile(String fileId) {
            // no-op for tests
        }
    }
}
