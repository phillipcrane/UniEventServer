package dk.unievent.app.infrastructure.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.unievent.app.infrastructure.config.SeaweedConfig;

@ExtendWith(MockitoExtension.class)
class SeaweedFsClientTests {

    private MockRestServiceServer server;

    private SeaweedFsClient seaweedFsClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();

        SeaweedConfig config = new SeaweedConfig();
        config.setMasterUrl("localhost:9333");
        seaweedFsClient = new SeaweedFsClient(config, new ObjectMapper());
    }

    @Test
    void assignFileShouldFailWhenMasterReturnsNonOk() {
        server.expect(requestTo("http://localhost:9333/dir/assign"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("boom"));

        IOException ex = assertThrows(IOException.class, () -> seaweedFsClient.assignFile());
        assertTrue(ex.getMessage().contains("Failed to get file assignment"));
    }

    @Test
    void assignFileShouldFailWhenResponseMissingFields() {
        server.expect(requestTo("http://localhost:9333/dir/assign"))
            .andRespond(withSuccess("{\"publicUrl\":\"localhost:8080\"}", MediaType.APPLICATION_JSON));

        IOException ex = assertThrows(IOException.class, () -> seaweedFsClient.assignFile());
        assertTrue(ex.getMessage().contains("missing fid/publicUrl"));
    }

    @Test
    void uploadFileShouldFailWhenVolumeRejectsUpload() {
        server.expect(requestTo("http://localhost:8080/1,abc"))
            .andRespond(withStatus(HttpStatus.BAD_GATEWAY).body("failed"));

        IOException ex = assertThrows(IOException.class,
                () -> seaweedFsClient.uploadFile("localhost:8080", "1,abc", "file.txt", "data".getBytes()));

        assertTrue(ex.getMessage().contains("Failed to upload file"));
    }

    @Test
    void downloadFileShouldFailWhenLookupHasNoLocations() {
        server.expect(requestTo("http://localhost:9333/dir/lookup?volumeId=1"))
            .andRespond(withSuccess("{\"locations\":[]}", MediaType.APPLICATION_JSON));

        IOException ex = assertThrows(IOException.class, () -> seaweedFsClient.downloadFile("1,abc"));
        assertTrue(ex.getMessage().contains("File location not found"));
    }

    @Test
    void downloadFileShouldReturnBytesWhenLookupAndDownloadSucceed() throws Exception {
        server.expect(requestTo("http://localhost:9333/dir/lookup?volumeId=1"))
            .andRespond(withSuccess("{\"locations\":[{\"publicUrl\":\"localhost:8080\"}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:8080/1,abc"))
            .andRespond(withSuccess("payload", MediaType.APPLICATION_OCTET_STREAM));

        byte[] result = seaweedFsClient.downloadFile("1,abc");

        assertArrayEquals("payload".getBytes(), result);
    }

    @Test
    void deleteFileShouldFailWhenLookupReturnsNotFound() {
        server.expect(requestTo("http://localhost:9333/dir/lookup?volumeId=1"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));

        IOException ex = assertThrows(IOException.class, () -> seaweedFsClient.deleteFile("1,abc"));
        assertTrue(ex.getMessage().contains("Could not find file to delete"));
    }
}
