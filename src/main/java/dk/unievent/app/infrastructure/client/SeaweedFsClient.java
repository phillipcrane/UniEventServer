package dk.unievent.app.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.unievent.app.infrastructure.config.SeaweedConfig;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@Component
public class SeaweedFsClient {

    private final RestClient.Builder restClientBuilder;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public SeaweedFsClient(SeaweedConfig config, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClientBuilder = restClientBuilder;
        this.restClient = restClientBuilder.baseUrl("http://" + config.getMasterUrl()).build();
        this.objectMapper = objectMapper;
    }

    public FileAssignment assignFile() throws IOException {
        log.debug("Requesting file assignment from SeaweedFS master");
        ResponseEntity<String> assignmentResponse;
        try {
            assignmentResponse = restClient.get()
                .uri("/dir/assign")
                .retrieve()
                .toEntity(String.class);
        } catch (RestClientException e) {
            log.error("Failed to get file assignment from SeaweedFS master", e);
            throw new IOException("Failed to get file assignment from SeaweedFS master", e);
        }

        if (assignmentResponse.getStatusCode() != HttpStatus.OK || assignmentResponse.getBody() == null) {
            log.error("Failed to get file assignment from SeaweedFS master: invalid response");
            throw new IOException("Failed to get file assignment from SeaweedFS master");
        }

        JsonNode assignmentNode = objectMapper.readTree(assignmentResponse.getBody());
        String fid = assignmentNode.path("fid").asText(null);
        String publicUrl = assignmentNode.path("publicUrl").asText(null);
        if (fid == null || publicUrl == null) {
            log.error("SeaweedFS assignment response missing fid/publicUrl");
            throw new IOException("SeaweedFS assignment response missing fid/publicUrl");
        }

        log.debug("File assignment successful: fid={}", fid);
        return new FileAssignment(fid, publicUrl);
    }

    public void uploadFile(String publicUrl, String fid, String filename, byte[] bytes) throws IOException {
        log.info("Uploading file to SeaweedFS: filename={}, fid={}, size={} bytes", filename, fid, bytes.length);
        String uploadPath = "/" + fid;

        org.springframework.util.LinkedMultiValueMap<String, Object> body =
            new org.springframework.util.LinkedMultiValueMap<>();
        body.add("file", new org.springframework.core.io.ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        ResponseEntity<String> uploadResponse;
        try {
            RestClient uploadClient = restClientBuilder.baseUrl("http://" + publicUrl).build();
            uploadResponse = uploadClient.post()
                .uri(uploadPath)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toEntity(String.class);
        } catch (RestClientException e) {
            log.error("Failed to upload file to SeaweedFS volume server: fid={}", fid, e);
            throw new IOException("Failed to upload file to SeaweedFS volume server", e);
        }

        if (uploadResponse.getStatusCode() != HttpStatus.CREATED && uploadResponse.getStatusCode() != HttpStatus.OK) {
            log.error("Failed to upload file to SeaweedFS volume server: status={}", uploadResponse.getStatusCode());
            throw new IOException("Failed to upload file to SeaweedFS volume server");
        }
        log.info("File uploaded successfully: fid={}", fid);
    }

    public byte[] downloadFile(String fileId) throws IOException {
        log.debug("Downloading file from SeaweedFS: fileId={}", fileId);
        ResponseEntity<String> lookupResponse;
        try {
            lookupResponse = restClient.get()
                .uri("/dir/lookup?volumeId={volumeId}", extractVolumeId(fileId))
                .retrieve()
                .toEntity(String.class);
        } catch (RestClientException e) {
            log.warn("File not found in SeaweedFS: {}", fileId, e);
            throw new IOException("File not found in SeaweedFS: " + fileId, e);
        }

        if (lookupResponse.getStatusCode() != HttpStatus.OK || lookupResponse.getBody() == null) {
            log.warn("File not found in SeaweedFS: {}", fileId);
            throw new IOException("File not found in SeaweedFS: " + fileId);
        }

        JsonNode lookupNode = objectMapper.readTree(lookupResponse.getBody());
        JsonNode locations = lookupNode.get("locations");
        if (locations == null || locations.size() == 0) {
            log.warn("File location not found in SeaweedFS: {}", fileId);
            throw new IOException("File location not found in SeaweedFS: " + fileId);
        }

        String volumePublicUrl = locations.get(0).path("publicUrl").asText(null);
        if (volumePublicUrl == null) {
            log.error("Invalid SeaweedFS lookup response for file: {}", fileId);
            throw new IOException("Invalid SeaweedFS lookup response for file: " + fileId);
        }

        ResponseEntity<byte[]> downloadResponse;
        try {
            downloadResponse = restClientBuilder
                .baseUrl("http://" + volumePublicUrl)
                .build()
                .get()
                .uri("/" + fileId)
                .retrieve()
                .toEntity(byte[].class);
        } catch (RestClientException e) {
            log.error("Could not read file: {}", fileId, e);
            throw new IOException("Could not read file: " + fileId, e);
        }
        if (downloadResponse.getStatusCode() != HttpStatus.OK || downloadResponse.getBody() == null) {
            log.error("Could not read file: {}", fileId);
            throw new IOException("Could not read file: " + fileId);
        }

        log.debug("File downloaded successfully: fileId={}, size={} bytes", fileId, downloadResponse.getBody().length);
        return downloadResponse.getBody();
    }

    public void deleteFile(String fileId) throws IOException {
        log.debug("Deleting file from SeaweedFS: fileId={}", fileId);
        ResponseEntity<String> lookupResponse;
        try {
            lookupResponse = restClient.get()
                .uri("/dir/lookup?volumeId={volumeId}", extractVolumeId(fileId))
                .retrieve()
                .toEntity(String.class);
        } catch (RestClientException e) {
            log.error("Could not find file to delete: {}", fileId, e);
            throw new IOException("Could not find file to delete: " + fileId, e);
        }

        if (lookupResponse.getStatusCode() != HttpStatus.OK || lookupResponse.getBody() == null) {
            log.error("Could not find file to delete: {}", fileId);
            throw new IOException("Could not find file to delete: " + fileId);
        }

        JsonNode lookupNode = objectMapper.readTree(lookupResponse.getBody());
        JsonNode locations = lookupNode.get("locations");
        if (locations == null || locations.size() == 0) {
            log.error("File location not found for delete: {}", fileId);
            throw new IOException("File location not found for delete: " + fileId);
        }

        String volumePublicUrl = locations.get(0).path("publicUrl").asText(null);
        if (volumePublicUrl == null) {
            log.error("Invalid SeaweedFS lookup response for delete: {}", fileId);
            throw new IOException("Invalid SeaweedFS lookup response for delete: " + fileId);
        }

        try {
            restClientBuilder
                .baseUrl("http://" + volumePublicUrl)
                .build()
                .delete()
                .uri("/" + fileId)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Could not delete file: {}", fileId, e);
            throw new IOException("Could not delete file: " + fileId, e);
        }
        log.info("File deleted successfully: {}", fileId);
    }

    private String extractVolumeId(String fid) {
        int commaIndex = fid.indexOf(',');
        return commaIndex > 0 ? fid.substring(0, commaIndex) : fid;
    }

    public record FileAssignment(String fid, String publicUrl) {}
}
