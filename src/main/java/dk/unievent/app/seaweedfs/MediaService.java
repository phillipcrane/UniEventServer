package dk.unievent.app.seaweedfs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.unievent.app.seaweedfs.MediaConfig;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

@Service
public class MediaService {

    private final String masterUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MediaService(MediaConfig config) {
        this.masterUrl = config.getMasterUrl();
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Store a file in SeaweedFS and return its file ID (fid)
     * 
     * Process:
     * 1. Get assignment from master server
     * 2. Upload file to volume server
     * 3. Return the file ID
     */
    public String store(MultipartFile file) throws IOException {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file " + filename);
        }
        if (filename.contains("..")) {
            throw new IOException("Cannot store file with relative path outside current directory " + filename);
        }

        try {
            // Step 1: Get assignment from SeaweedFS master
            String assignmentUrl = "http://" + masterUrl + "/dir/assign";
            ResponseEntity<String> assignmentResponse = restTemplate.getForEntity(assignmentUrl, String.class);
            
            if (assignmentResponse.getStatusCode() != HttpStatus.OK) {
                throw new IOException("Failed to get file assignment from SeaweedFS master");
            }

            JsonNode assignmentNode = objectMapper.readTree(assignmentResponse.getBody());
            String fid = assignmentNode.get("fid").asText();
            String publicUrl = assignmentNode.get("publicUrl").asText();

            // Step 2: Upload file to volume server
            String uploadUrl = "http://" + publicUrl + "/" + fid;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            // Create multipart request
            org.springframework.util.LinkedMultiValueMap<String, Object> body = 
                new org.springframework.util.LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
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

            if (uploadResponse.getStatusCode() != HttpStatus.CREATED && 
                uploadResponse.getStatusCode() != HttpStatus.OK) {
                throw new IOException("Failed to upload file to SeaweedFS volume server");
            }

            return fid;
        } catch (Exception e) {
            throw new IOException("Error uploading file to SeaweedFS: " + e.getMessage(), e);
        }
    }

    /**
     * Load file ID (not used in current API, kept for potential internal use)
     */
    public Path load(String filename) {
        return Path.of(filename);
    }

    /**
     * Load and return file as a Resource for download
     */
    public Resource loadAsResource(String fileId) throws IOException {
        if (fileId == null || fileId.isEmpty()) {
            throw new IOException("Invalid file ID");
        }

        try {
            // Query master to find which volume server has the file
            String lookupUrl = "http://" + masterUrl + "/dir/lookup?volumeId=" + extractVolumeId(fileId);
            ResponseEntity<String> lookupResponse = restTemplate.getForEntity(lookupUrl, String.class);
            
            if (lookupResponse.getStatusCode() != HttpStatus.OK) {
                throw new IOException("File not found in SeaweedFS: " + fileId);
            }

            JsonNode lookupNode = objectMapper.readTree(lookupResponse.getBody());
            JsonNode locations = lookupNode.get("locations");
            
            if (locations == null || locations.size() == 0) {
                throw new IOException("File location not found in SeaweedFS: " + fileId);
            }

            // Use the first available volume
            String volumePublicUrl = locations.get(0).get("publicUrl").asText();
            String downloadUrl = "http://" + volumePublicUrl + "/" + fileId;

            // Download the file
            ResponseEntity<byte[]> downloadResponse = restTemplate.getForEntity(downloadUrl, byte[].class);
            
            if (downloadResponse.getStatusCode() != HttpStatus.OK || downloadResponse.getBody() == null) {
                throw new IOException("Could not read file: " + fileId);
            }

            return new ByteArrayResource(downloadResponse.getBody());
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Could not read file: " + fileId, e);
        }
    }

    /**
     * Delete a file from SeaweedFS
     */
    public void delete(String fileId) throws IOException {
        if (fileId == null || fileId.isEmpty()) {
            throw new IOException("Invalid file ID");
        }

        try {
            // Query master to find volume server
            String lookupUrl = "http://" + masterUrl + "/dir/lookup?volumeId=" + extractVolumeId(fileId);
            ResponseEntity<String> lookupResponse = restTemplate.getForEntity(lookupUrl, String.class);
            
            if (lookupResponse.getStatusCode() != HttpStatus.OK) {
                System.err.println("Warning: Could not find file to delete: " + fileId);
                return;
            }

            JsonNode lookupNode = objectMapper.readTree(lookupResponse.getBody());
            JsonNode locations = lookupNode.get("locations");
            
            if (locations != null && locations.size() > 0) {
                String volumePublicUrl = locations.get(0).get("publicUrl").asText();
                String deleteUrl = "http://" + volumePublicUrl + "/" + fileId;
                
                restTemplate.delete(deleteUrl);
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not delete file from SeaweedFS: " + fileId);
            // Don't fail hard—file might already be deleted
        }
    }

    /**
     * Extract volume ID from file ID (fid format: volumeId,key)
     */
    private String extractVolumeId(String fid) {
        int commaIndex = fid.indexOf(',');
        return commaIndex > 0 ? fid.substring(0, commaIndex) : fid;
    }
}
