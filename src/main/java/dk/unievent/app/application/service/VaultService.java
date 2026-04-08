package dk.unievent.app.application.service;

import dk.unievent.app.application.dto.SecretDTO;
import dk.unievent.app.application.mapper.SecretMapper;
import dk.unievent.app.db.model.SecretEntity;
import dk.unievent.app.db.repository.SecretRepository;
import dk.unievent.app.infrastructure.client.VaultClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        return vaultClient.readSecretData();
    }

    public String readVaultSecretValue(String key) {
        return vaultClient.readSecretValue(key);
    }

    public List<SecretDTO> getAllSecrets() {
        return secretRepository.findAll().stream()
            .map(secretMapper::toDTO)
            .collect(Collectors.toList());
    }

    public Optional<SecretDTO> getSecretById(Long id) {
        return secretRepository.findById(id).map(secretMapper::toDTO);
    }

    public Optional<SecretDTO> getSecretByName(String name) {
        return secretRepository.findByName(name).map(secretMapper::toDTO);
    }

    public SecretDTO saveSecret(SecretDTO secretDTO) {
        SecretEntity entity = secretMapper.toEntity(secretDTO);
        entity.setLastSyncedAt(LocalDateTime.now());
        SecretEntity saved = secretRepository.save(entity);
        return secretMapper.toDTO(saved);
    }

    public boolean deleteSecret(Long id) {
        if (!secretRepository.existsById(id)) {
            return false;
        }

        secretRepository.deleteById(id);
        return true;
    }
}
