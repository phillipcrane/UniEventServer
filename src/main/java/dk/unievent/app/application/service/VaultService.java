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

    private static final String SECRET_TYPE_FACEBOOK_PAGE_TOKEN = "facebook_page_token";
    private static final String SECRET_KEY_ACCESS_TOKEN = "access_token";

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

    // ── Vault raw read ────────────────────────────────────────────────────────

    public Map<String, String> readVaultSecretData() {
        log.debug("Reading all secret data from Vault");
        return vaultClient.readSecretData();
    }

    public String readVaultSecretValue(String key) {
        log.debug("Reading secret value from Vault");
        return vaultClient.readSecretValue(key);
    }

    // ── Secrets registry CRUD ─────────────────────────────────────────────────

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

    public org.springframework.data.domain.Page<SecretDTO> getSecretsByType(String secretType, org.springframework.data.domain.Pageable pageable) {
        return secretRepository.findBySecretTypeOrderByNameAsc(secretType, pageable).map(secretMapper::toDTO);
    }

    public org.springframework.data.domain.Page<SecretDTO> getSecretsByStatus(String status, org.springframework.data.domain.Pageable pageable) {
        return secretRepository.findByStatusOrderByNameAsc(status, pageable).map(secretMapper::toDTO);
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

    // ── Facebook page token operations ────────────────────────────────────────

    /**
     * Store a Facebook page access token in Vault and register it in the secrets registry.
     * Token stored at: secret/data/unievent/facebook/page_{pageId}
     */
    public void storePageToken(String pageId, String token) {
        try {
            log.debug("Storing Facebook page token for page: {} (token: {})", pageId, maskToken(token));

            String vaultPath = String.format("/v1/secret/data/unievent/facebook/page_%s", pageId);

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
                upsertPageTokenRecord(pageId, "active", null);
            } else {
                throw new RuntimeException("Failed to store page token in Vault: unexpected status " + response.getStatusCode());
            }

        } catch (RestClientResponseException e) {
            log.error("Failed to store Facebook page token in Vault for page: {}. Status: {} - {}", pageId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw e;
        } catch (Exception e) {
            log.error("Error storing Facebook page token in Vault for page: {}", pageId, e);
            throw new RuntimeException("Unexpected error storing page token: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieve a Facebook page access token from Vault.
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
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Error retrieving Facebook page token for page: {}", pageId, e);
            return Optional.empty();
        }
    }

    /**
     * Update a Facebook page access token in Vault and sync the secrets registry record.
     */
    public void updatePageToken(String pageId, String newToken) {
        try {
            log.debug("Updating Facebook page token for page: {} (token: {})", pageId, maskToken(newToken));

            String vaultPath = String.format("/v1/secret/data/unievent/facebook/page_%s", pageId);

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
                upsertPageTokenRecord(pageId, "active", null);
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

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Set the expiry on an existing registry record (call after storePageToken once the
     * PageEntity expiration is known). Never throws.
     */
    public void setPageTokenExpiry(String pageId, LocalDateTime expiresAt) {
        upsertPageTokenRecord(pageId, "active", expiresAt);
    }

    /**
     * Mark a page token as errored in the registry (e.g. after a failed refresh).
     * Never throws — registry sync must never break the caller's flow.
     */
    public void markPageTokenError(String pageId) {
        upsertPageTokenRecord(pageId, "error", null);
    }

    /**
     * Mark a page token as inactive in the registry (e.g. when a page is deleted).
     * Never throws — registry sync must never break the caller's flow.
     */
    public void markPageTokenInactive(String pageId) {
        upsertPageTokenRecord(pageId, "inactive", null);
    }

    /**
     * Mark a page token as invalid in the registry (e.g. Facebook rejected it during ingestion).
     * Never throws — registry sync must never break the caller's flow.
     */
    public void markPageTokenInvalid(String pageId) {
        upsertPageTokenRecord(pageId, "invalid", null);
    }

    /**
     * Creates or updates the secrets registry row for a Facebook page token.
     * The name "facebook_page_{pageId}" is the stable unique key for lookups.
     * expiresAt may be null when not yet known.
     */
    private void upsertPageTokenRecord(String pageId, String status, LocalDateTime expiresAt) {
        try {
            String name = "facebook_page_" + pageId;
            String vaultPath = "secret/data/unievent/facebook/page_" + pageId;

            SecretEntity record = secretRepository.findByName(name)
                    .orElseGet(() -> SecretEntity.builder()
                            .name(name)
                            .secretType(SECRET_TYPE_FACEBOOK_PAGE_TOKEN)
                            .vaultPath(vaultPath)
                            .vaultKey(SECRET_KEY_ACCESS_TOKEN)
                            .build());

            record.setStatus(status);
            record.setLastSyncedAt(LocalDateTime.now());
            if (expiresAt != null) {
                record.setExpiresAt(expiresAt);
            }
            secretRepository.save(record);
            log.debug("Secrets registry updated for page: {} (status: {}, expiresAt: {})", pageId, status, expiresAt);
        } catch (Exception e) {
            // Registry sync failure must never break the Vault operation that already succeeded.
            log.warn("Failed to update secrets registry for page: {} — {}", pageId, e.getMessage());
        }
    }

    private static String maskToken(String token) {
        if (token == null || token.isEmpty()) return "[EMPTY]";
        int visible = Math.min(4, token.length() - 1);
        return (visible > 0 ? token.substring(0, visible) : "") + "***";
    }
}
