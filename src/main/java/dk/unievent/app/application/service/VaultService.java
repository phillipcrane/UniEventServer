package dk.unievent.app.application.service;

import dk.unievent.app.application.dto.SecretDTO;
import dk.unievent.app.application.mapper.SecretMapper;
import dk.unievent.app.db.model.SecretEntity;
import dk.unievent.app.db.repository.SecretRepository;
import dk.unievent.app.infrastructure.client.VaultClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "unievent.vault", name = "enabled", havingValue = "true")
public class VaultService {

    private final VaultClient vaultClient;
    private final SecretRepository secretRepository;
    private final SecretMapper secretMapper;
    private final RestClient restClient;

    public VaultService(
        VaultClient vaultClient,
        SecretRepository secretRepository,
        SecretMapper secretMapper,
        RestClient.Builder restClientBuilder,
        dk.unievent.app.infrastructure.config.VaultConfig vaultConfig
    ) {
        this.vaultClient = vaultClient;
        this.secretRepository = secretRepository;
        this.secretMapper = secretMapper;

        RestClient.Builder builder = restClientBuilder
            .baseUrl(vaultConfig.getUri())
            .defaultHeader("X-Vault-Token", vaultConfig.getToken());

        if (vaultConfig.getCaCertPath() != null && !vaultConfig.getCaCertPath().isBlank()) {
            try {
                builder = builder.requestFactory(buildTlsRequestFactory(vaultConfig.getCaCertPath()));
                log.info("VaultService TLS configured with CA cert: {}", vaultConfig.getCaCertPath());
            } catch (Exception e) {
                throw new RuntimeException("Failed to configure Vault TLS from: " + vaultConfig.getCaCertPath(), e);
            }
        }

        this.restClient = builder.build();
    }

    private JdkClientHttpRequestFactory buildTlsRequestFactory(String caCertPath) throws Exception {
        try (InputStream certStream = Files.newInputStream(Paths.get(caCertPath))) {
            X509Certificate caCert = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(certStream);

            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            trustStore.setCertificateEntry("vault-ca", caCert);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            HttpClient httpClient = HttpClient.newBuilder().sslContext(sslContext).build();
            return new JdkClientHttpRequestFactory(httpClient);
        }
    }

    public Map<String, String> readVaultSecretData() {
        log.debug("Reading all secret data from Vault");
        return vaultClient.readSecretData();
    }

    public String readVaultSecretValue(String key) {
        log.debug("Reading secret value from Vault");
        return vaultClient.readSecretValue(key);
    }

    public List<SecretDTO> getAllSecrets() {
        log.debug("Fetching all secrets from repository");
        List<SecretDTO> secrets = secretRepository.findAll().stream()
            .map(secretMapper::toDTO)
            .collect(Collectors.toList());
        log.debug("Found {} secrets", secrets.size());
        return secrets;
    }

    public Optional<SecretDTO> getSecretById(Long id) {
        log.debug("Fetching secret with id: {}", id);
        return secretRepository.findById(id).map(secretMapper::toDTO);
    }

    public Optional<SecretDTO> getSecretByName(String name) {
        log.debug("Looking up secret in repository");
        return secretRepository.findByName(name).map(secretMapper::toDTO);
    }

    public SecretDTO saveSecret(SecretDTO secretDTO) {
        log.debug("Saving secret to repository");
        SecretEntity entity = secretMapper.toEntity(secretDTO);
        entity.setLastSyncedAt(LocalDateTime.now());
        SecretEntity saved = secretRepository.save(entity);
        log.info("Secret saved successfully");
        return secretMapper.toDTO(saved);
    }

    public boolean deleteSecret(Long id) {
        log.debug("Deleting secret with id: {}", id);
        if (!secretRepository.existsById(id)) {
            log.warn("Secret not found with id: {}", id);
            return false;
        }

        secretRepository.deleteById(id);
        log.info("Secret deleted successfully with id: {}", id);
        return true;
    }

    /**
     * Store a Facebook page access token in Vault.
     * Token stored at: secret/data/unievent/facebook/page_{pageId}
     * 
     * @param pageId Facebook page ID
     * @param token Page access token to store
     */
    public void storePageToken(String pageId, String token) {
        try {
            log.debug("Storing Facebook page token for page: {} (token: {})", pageId, maskToken(token));
            
            String vaultPath = String.format("/v1/secret/data/unievent/facebook/page_%s", pageId);
            
            // Build Vault request body
            Map<String, Object> vaultData = Map.of(
                "data", Map.of(
                    "access_token", token,
                    "stored_at", LocalDateTime.now().toString()
                )
            );
            
            var response = restClient.post()
                    .uri(vaultPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(vaultData)
                    .retrieve()
                    .toEntity(Object.class);
            
            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.info("Facebook page token stored successfully for page: {}", pageId);
            } else {
                throw new RuntimeException("Failed to store page token in Vault: unexpected status " + response.getStatusCode());
            }
            
        } catch (RestClientResponseException e) {
            log.error("Failed to store Facebook page token in Vault for page: {}. Status: {} - {}", pageId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw e;  // Re-throw since this is a critical operation
        } catch (Exception e) {
            log.error("Error storing Facebook page token in Vault for page: {}", pageId, e);
            throw new RuntimeException("Unexpected error storing page token: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieve a Facebook page access token from Vault.
     * 
     * @param pageId Facebook page ID
     * @return Optional containing the access token if found
     */
    @SuppressWarnings("unchecked")
    public Optional<String> getPageToken(String pageId) {
        try {
            log.debug("Retrieving Facebook page token for page: {}", pageId);
            
            String vaultPath = String.format("/v1/secret/data/unievent/facebook/page_%s", pageId);
            
            var response = restClient.get()
                    .uri(vaultPath)
                    .retrieve()
                    .body(Object.class);
            
            if (response == null) {
                log.warn("No token found in Vault for page: {}", pageId);
                return Optional.empty();
            }
            
            // Parse Vault response structure: {data: {data: {access_token: "..."}}}
            Map<String, Object> responseMap = (Map<String, Object>) response;
            Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
            
            if (dataMap == null) {
                log.warn("Invalid Vault response structure for page: {}", pageId);
                return Optional.empty();
            }
            
            Map<String, Object> tokenData = (Map<String, Object>) dataMap.get("data");
            if (tokenData != null && tokenData.containsKey("access_token")) {
                String token = (String) tokenData.get("access_token");
                log.debug("Facebook page token retrieved successfully for page: {} (token: {})", pageId, maskToken(token));
                return Optional.of(token);
            }
            
            log.warn("No access_token in Vault data for page: {}", pageId);
            return Optional.empty();
            
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                log.debug("Token not found in Vault for page: {}", pageId);
                return Optional.empty();
            }
            log.warn("Failed to retrieve Facebook page token for page: {}. Status: {} - {}", pageId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return Optional.empty();  // Return empty instead of throwing to prevent entire ingestion from failing
        } catch (Exception e) {
            log.warn("Error retrieving Facebook page token for page: {}", pageId, e);
            return Optional.empty();  // Return empty instead of throwing
        }
    }

    /**
     * Update a Facebook page access token in Vault.
     * 
     * @param pageId Facebook page ID
     * @param newToken New page access token
     */
    public void updatePageToken(String pageId, String newToken) {
        try {
            log.debug("Updating Facebook page token for page: {} (token: {})", pageId, maskToken(newToken));
            
            String vaultPath = String.format("/v1/secret/data/unievent/facebook/page_%s", pageId);
            
            // Build Vault request body
            Map<String, Object> vaultData = Map.of(
                "data", Map.of(
                    "access_token", newToken,
                    "updated_at", LocalDateTime.now().toString()
                )
            );
            
            var response = restClient.post()
                    .uri(vaultPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(vaultData)
                    .retrieve()
                    .toEntity(Object.class);
            
            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.info("Facebook page token updated successfully for page: {}", pageId);
            } else {
                log.error("Failed to update token: unexpected status {}", response.getStatusCode());
            }
            
        } catch (RestClientResponseException e) {
            log.error("Failed to update Facebook page token in Vault for page: {}. Status: {}", pageId, e.getStatusCode(), e);
            throw new RuntimeException("Failed to update page token in Vault: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error updating Facebook page token in Vault for page: {}", pageId, e);
            throw new RuntimeException("Unexpected error updating page token: " + e.getMessage(), e);
        }
    }

    private static String maskToken(String token) {
        if (token == null || token.isEmpty()) return "[EMPTY]";
        int visible = Math.min(4, token.length() - 1);
        return (visible > 0 ? token.substring(0, visible) : "") + "***";
    }
}
