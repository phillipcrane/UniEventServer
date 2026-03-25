package dk.unievent.secretmanager;

import com.google.cloud.secretmanager.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/secrets")
/**
 * Manages page access tokens in Google Secret Manager.
 */
public class SecretManagerController {

    private final SecretManagerServiceClient client;

    @Value("${gcp.project.id}")
    private String projectId;

    public SecretManagerController() throws IOException {
        this.client = SecretManagerServiceClient.create();
    }

    /**
     * Creates or updates the latest token version for a page.
     */
    @PostMapping("/pages/{pageId}/token")
    public ResponseEntity<Map<String, Object>> addPageToken(
            @PathVariable String pageId,
            @RequestBody Map<String, Object> request) {
        try {
            String token = (String) request.get("token");
            Long expiresIn = ((Number) request.get("expiresIn")).longValue();

            if (token == null || token.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing token parameter"));
            }

            String secretId = "facebook-token-" + pageId;
            String parent = ProjectName.format(projectId);

            // Create secret if it doesn't exist
            try {
                client.createSecret(parent, secretId, Secret.newBuilder().build());
            } catch (Exception e) {
                if (!e.getMessage().contains("Already exists")) {
                    throw e;
                }
            }

            // Add version
            String secretVersionName = SecretVersionName.of(projectId, secretId, "latest").toString();
            client.addSecretVersion(secretVersionName, SecretPayload.newBuilder()
                .setData(ByteString.copyFromUtf8(token))
                .build());

            return ResponseEntity.ok(Map.of("success", true, "message", "Token stored successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Alias for token upsert to keep write semantics explicit for clients.
     */
    @PutMapping("/pages/{pageId}/token")
    public ResponseEntity<Map<String, Object>> updatePageToken(
            @PathVariable String pageId,
            @RequestBody Map<String, Object> request) {
        // Update is the same as add for latest version
        return addPageToken(pageId, request);
    }

    /**
     * Reads the latest token value for a page.
     */
    @GetMapping("/pages/{pageId}/token")
    public ResponseEntity<?> getPageToken(@PathVariable String pageId) {
        try {
            String secretId = "facebook-token-" + pageId;
            String secretVersionName = SecretVersionName.of(projectId, secretId, "latest").toString();

            AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);
            String token = response.getPayload().getData().toStringUtf8();

            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception e) {
            if (e.getMessage().contains("NOT_FOUND")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Placeholder for expiry-check semantics.
     */
    @GetMapping("/pages/{pageId}/token/status")
    public ResponseEntity<Map<String, Object>> checkTokenExpiry(@PathVariable String pageId) {
        // Simplified implementation - in a real scenario, you'd check expiry
        return ResponseEntity.ok(Map.of("expired", false, "message", "Token expiry check not implemented"));
    }
}