package dk.unievent.app.vault;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads secrets from HashiCorp Vault using the KV v2 HTTP API.
 * Only active when unievent.vault.enabled=true.
 *
 * Usage: inject VaultClient and call readSecret("my-key") or readSecrets().
 */
@Component
@ConditionalOnProperty(prefix = "unievent.vault", name = "enabled", havingValue = "true")
public class VaultClient {

    private final VaultProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public VaultClient(VaultProperties properties) {
        this(properties, new RestTemplate());
    }

    // Package-private for testing with MockRestServiceServer
    VaultClient(VaultProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Reads all secrets from the configured KV v2 path.
     * Vault KV v2 response shape: { data: { data: { key: value, ... } } }
     */
    public Map<String, String> readSecrets() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Vault-Token", properties.getToken());
            HttpEntity<Void> request = new HttpEntity<>(headers);

            String url = properties.getUri() + "/v1/" + properties.getSecretPath();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                return Collections.emptyMap();
            }

            JsonNode data = objectMapper.readTree(response.getBody()).path("data").path("data");
            if (data.isMissingNode()) {
                return Collections.emptyMap();
            }

            Map<String, String> secrets = new HashMap<>();
            data.properties().forEach(e -> secrets.put(e.getKey(), e.getValue().asText()));
            return secrets;

        } catch (Exception e) {
            throw new RuntimeException("Failed to read secrets from Vault: " + properties.getUri(), e);
        }
    }

    /**
     * Reads a single secret by key. Returns null if not found.
     */
    public String readSecret(String key) {
        return readSecrets().getOrDefault(key, null);
    }
}
