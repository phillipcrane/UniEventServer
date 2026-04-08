package dk.unievent.app.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.unievent.app.infrastructure.config.SeaweedConfig;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Component
public class SeaweedFsClient {

    private final String masterUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SeaweedFsClient(SeaweedConfig config, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.masterUrl = config.getMasterUrl();
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public FileAssignment assignFile() throws IOException {
        String assignmentUrl = "http://" + masterUrl + "/dir/assign";
        ResponseEntity<String> assignmentResponse = restTemplate.getForEntity(assignmentUrl, String.class);

        if (assignmentResponse.getStatusCode() != HttpStatus.OK || assignmentResponse.getBody() == null) {
            throw new IOException("Failed to get file assignment from SeaweedFS master");
        }

        JsonNode assignmentNode = objectMapper.readTree(assignmentResponse.getBody());
        String fid = assignmentNode.path("fid").asText(null);
        String publicUrl = assignmentNode.path("publicUrl").asText(null);
        if (fid == null || publicUrl == null) {
            throw new IOException("SeaweedFS assignment response missing fid/publicUrl");
        }

        return new FileAssignment(fid, publicUrl);
    }

    public void uploadFile(String publicUrl, String fid, String filename, byte[] bytes) throws IOException {
        String uploadUrl = "http://" + publicUrl + "/" + fid;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        org.springframework.util.LinkedMultiValueMap<String, Object> body =
            new org.springframework.util.LinkedMultiValueMap<>();
        body.add("file", new org.springframework.core.io.ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        HttpEntity<org.springframework.util.LinkedMultiValueMap<String, Object>> requestEntity =
            new HttpEntity<>(body, headers);

        ResponseEntity<String> uploadResponse = restTemplate.exchange(
            uploadUrl,
            HttpMethod.POST,
            requestEntity,
            String.class
        );

        if (uploadResponse.getStatusCode() != HttpStatus.CREATED && uploadResponse.getStatusCode() != HttpStatus.OK) {
            throw new IOException("Failed to upload file to SeaweedFS volume server");
        }
    }

    public byte[] downloadFile(String fileId) throws IOException {
        String lookupUrl = "http://" + masterUrl + "/dir/lookup?volumeId=" + extractVolumeId(fileId);
        ResponseEntity<String> lookupResponse = restTemplate.getForEntity(lookupUrl, String.class);

        if (lookupResponse.getStatusCode() != HttpStatus.OK || lookupResponse.getBody() == null) {
            throw new IOException("File not found in SeaweedFS: " + fileId);
        }

        JsonNode lookupNode = objectMapper.readTree(lookupResponse.getBody());
        JsonNode locations = lookupNode.get("locations");
        if (locations == null || locations.size() == 0) {
            throw new IOException("File location not found in SeaweedFS: " + fileId);
        }

        String volumePublicUrl = locations.get(0).path("publicUrl").asText(null);
        if (volumePublicUrl == null) {
            throw new IOException("Invalid SeaweedFS lookup response for file: " + fileId);
        }

        String downloadUrl = "http://" + volumePublicUrl + "/" + fileId;
        ResponseEntity<byte[]> downloadResponse = restTemplate.getForEntity(downloadUrl, byte[].class);
        if (downloadResponse.getStatusCode() != HttpStatus.OK || downloadResponse.getBody() == null) {
            throw new IOException("Could not read file: " + fileId);
        }

        return downloadResponse.getBody();
    }

    public void deleteFile(String fileId) throws IOException {
        String lookupUrl = "http://" + masterUrl + "/dir/lookup?volumeId=" + extractVolumeId(fileId);
        ResponseEntity<String> lookupResponse = restTemplate.getForEntity(lookupUrl, String.class);

        if (lookupResponse.getStatusCode() != HttpStatus.OK || lookupResponse.getBody() == null) {
            throw new IOException("Could not find file to delete: " + fileId);
        }

        JsonNode lookupNode = objectMapper.readTree(lookupResponse.getBody());
        JsonNode locations = lookupNode.get("locations");
        if (locations == null || locations.size() == 0) {
            throw new IOException("File location not found for delete: " + fileId);
        }

        String volumePublicUrl = locations.get(0).path("publicUrl").asText(null);
        if (volumePublicUrl == null) {
            throw new IOException("Invalid SeaweedFS lookup response for delete: " + fileId);
        }

        String deleteUrl = "http://" + volumePublicUrl + "/" + fileId;
        restTemplate.delete(deleteUrl);
    }

    private String extractVolumeId(String fid) {
        int commaIndex = fid.indexOf(',');
        return commaIndex > 0 ? fid.substring(0, commaIndex) : fid;
    }

    public record FileAssignment(String fid, String publicUrl) {}
}
