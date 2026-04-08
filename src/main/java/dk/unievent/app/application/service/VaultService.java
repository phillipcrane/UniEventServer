package dk.unievent.app.application.service;

import dk.unievent.app.application.dto.SecretDTO;
import dk.unievent.app.application.mapper.SecretMapper;
import dk.unievent.app.db.model.SecretEntity;
import dk.unievent.app.db.repository.SecretRepository;
import dk.unievent.app.infrastructure.client.VaultClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

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

    public VaultService(VaultClient vaultClient, SecretRepository secretRepository, SecretMapper secretMapper) {
        this.vaultClient = vaultClient;
        this.secretRepository = secretRepository;
        this.secretMapper = secretMapper;
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
}
