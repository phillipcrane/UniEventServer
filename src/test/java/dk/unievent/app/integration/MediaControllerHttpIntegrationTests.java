package dk.unievent.app.integration;

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
        String boundary = "----unievent-boundary";
        byte[] body = multipartBody(boundary, "file", "poster.png", "image/png", "png-bytes".getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/media")))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
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
        fakeSeaweed.failAssign = true;

        String boundary = "----unievent-boundary";
        byte[] body = multipartBody(boundary, "file", "poster.png", "image/png", "png-bytes".getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/media")))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
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
        List<Map<String, Object>> items = objectMapper.readValue(response.body(), new TypeReference<List<Map<String, Object>>>() {});

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
    void downloadMissingMediaShouldReturnGlobal500Shape() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/media/999999")))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> error = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});

        assertEquals(500, response.statusCode());
        assertEquals("Internal Server Error", error.get("error"));
        assertEquals(500, ((Number) error.get("status")).intValue());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
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
            super(config(), new org.springframework.web.client.RestTemplate(), new ObjectMapper());
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
