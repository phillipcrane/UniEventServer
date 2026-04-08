package dk.unievent.app.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.unievent.app.infrastructure.config.VaultConfig;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "unievent.vault", name = "enabled", havingValue = "true")
public class VaultClient {

    private final VaultConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public VaultClient(VaultConfig config, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.config = config;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Map<String, String> readSecretData() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Vault-Token", config.getToken());
            HttpEntity<Void> request = new HttpEntity<>(headers);

            String url = config.getUri() + "/v1/" + config.getSecretPath();
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
            throw new RuntimeException("Failed to read secrets from Vault: " + config.getUri(), e);
        }
    }

    public String readSecretValue(String key) {
        return readSecretData().getOrDefault(key, null);
    }
}
