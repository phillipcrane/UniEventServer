package dk.unievent.app.infrastructure.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.unievent.app.infrastructure.config.SeaweedConfig;

@ExtendWith(MockitoExtension.class)
class SeaweedFsClientTests {

    @Mock
    private RestTemplate restTemplate;

    private SeaweedFsClient seaweedFsClient;

    @BeforeEach
    void setUp() {
        SeaweedConfig config = new SeaweedConfig();
        config.setMasterUrl("localhost:9333");
        seaweedFsClient = new SeaweedFsClient(config, restTemplate, new ObjectMapper());
    }

    @Test
    void assignFileShouldFailWhenMasterReturnsNonOk() {
        when(restTemplate.getForEntity("http://localhost:9333/dir/assign", String.class))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("boom"));

        IOException ex = assertThrows(IOException.class, () -> seaweedFsClient.assignFile());
        assertTrue(ex.getMessage().contains("Failed to get file assignment"));
    }

    @Test
    void assignFileShouldFailWhenResponseMissingFields() {
        when(restTemplate.getForEntity("http://localhost:9333/dir/assign", String.class))
                .thenReturn(ResponseEntity.ok("{\"publicUrl\":\"localhost:8080\"}"));

        IOException ex = assertThrows(IOException.class, () -> seaweedFsClient.assignFile());
        assertTrue(ex.getMessage().contains("missing fid/publicUrl"));
    }

    @Test
    void uploadFileShouldFailWhenVolumeRejectsUpload() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
            .thenReturn(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("failed"));

        IOException ex = assertThrows(IOException.class,
                () -> seaweedFsClient.uploadFile("localhost:8080", "1,abc", "file.txt", "data".getBytes()));

        assertTrue(ex.getMessage().contains("Failed to upload file"));
    }

    @Test
    void downloadFileShouldFailWhenLookupHasNoLocations() {
        when(restTemplate.getForEntity("http://localhost:9333/dir/lookup?volumeId=1", String.class))
                .thenReturn(ResponseEntity.ok("{\"locations\":[]}"));

        IOException ex = assertThrows(IOException.class, () -> seaweedFsClient.downloadFile("1,abc"));
        assertTrue(ex.getMessage().contains("File location not found"));
    }

    @Test
    void downloadFileShouldReturnBytesWhenLookupAndDownloadSucceed() throws Exception {
        when(restTemplate.getForEntity("http://localhost:9333/dir/lookup?volumeId=1", String.class))
                .thenReturn(ResponseEntity.ok("{\"locations\":[{\"publicUrl\":\"localhost:8080\"}]}"));
        when(restTemplate.getForEntity("http://localhost:8080/1,abc", byte[].class))
                .thenReturn(ResponseEntity.ok("payload".getBytes()));

        byte[] result = seaweedFsClient.downloadFile("1,abc");

        assertArrayEquals("payload".getBytes(), result);
    }

    @Test
    void deleteFileShouldFailWhenLookupMissingBody() {
        when(restTemplate.getForEntity("http://localhost:9333/dir/lookup?volumeId=1", String.class))
                .thenReturn(ResponseEntity.ok(null));

        IOException ex = assertThrows(IOException.class, () -> seaweedFsClient.deleteFile("1,abc"));
        assertTrue(ex.getMessage().contains("Could not find file to delete"));
    }
}
