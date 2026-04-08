package dk.unievent.app.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.unievent.app.infrastructure.config.VaultConfig;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "unievent.vault", name = "enabled", havingValue = "true")
public class VaultClient {

    private final VaultConfig config;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public VaultClient(VaultConfig config, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.config = config;
        this.restClient = restClientBuilder
            .baseUrl(config.getUri())
            .defaultHeader("X-Vault-Token", config.getToken())
            .build();
        this.objectMapper = objectMapper;
    }

    public Map<String, String> readSecretData() {
        log.debug("Accessing Vault");
        try {
            ResponseEntity<String> response = restClient.get()
                .uri("/v1/" + config.getSecretPath())
                .retrieve()
                .toEntity(String.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.warn("Invalid response from Vault: status={}", response.getStatusCode());
                return Collections.emptyMap();
            }

            JsonNode data = objectMapper.readTree(response.getBody()).path("data").path("data");
            if (data.isMissingNode()) {
                log.warn("No secret data found in Vault response");
                return Collections.emptyMap();
            }

            Map<String, String> secrets = new HashMap<>();
            data.properties().forEach(e -> secrets.put(e.getKey(), e.getValue().asText()));
            log.info("Successfully read {} secrets from Vault", secrets.size());
            return secrets;
        } catch (RestClientResponseException e) {
            log.error("Failed to read secrets from Vault: {} (status: {})", config.getUri(), e.getStatusCode(), e);
            throw new RuntimeException(
                "Failed to read secrets from Vault: " + config.getUri() + " (status: " + e.getStatusCode() + ")",
                e
            );
        } catch (Exception e) {
            log.error("Failed to read secrets from Vault: {}", config.getUri(), e);
            throw new RuntimeException("Failed to read secrets from Vault: " + config.getUri(), e);
        }
    }

    public String readSecretValue(String key) {
        return readSecretData().getOrDefault(key, null);
    }
}
