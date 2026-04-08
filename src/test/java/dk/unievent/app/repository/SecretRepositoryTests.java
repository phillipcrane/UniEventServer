package dk.unievent.app.repository;

import dk.unievent.app.db.model.SecretEntity;
import dk.unievent.app.db.repository.SecretRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SecretRepositoryTests {

    @Autowired
    private SecretRepository secretRepository;

    @BeforeEach
    void setUp() {
        SecretEntity first = SecretEntity.builder()
            .name("database-password")
            .secretType("database")
            .vaultPath("secret/data/db")
            .vaultKey("password")
            .status("active")
            .build();

        SecretEntity second = SecretEntity.builder()
            .name("api-token")
            .secretType("api")
            .vaultPath("secret/data/api")
            .vaultKey("token")
            .status("active")
            .build();

        SecretEntity third = SecretEntity.builder()
            .name("legacy-key")
            .secretType("api")
            .vaultPath("secret/data/legacy")
            .vaultKey("key")
            .status("inactive")
            .build();

        secretRepository.save(first);
        secretRepository.save(second);
        secretRepository.save(third);
    }

    @Test
    void testFindByName() {
        Optional<SecretEntity> found = secretRepository.findByName("api-token");

        assertTrue(found.isPresent());
        assertEquals("api", found.get().getSecretType());
        assertEquals("secret/data/api", found.get().getVaultPath());
    }

    @Test
    void testFindByNameNotFound() {
        Optional<SecretEntity> found = secretRepository.findByName("missing-name");

        assertTrue(found.isEmpty());
    }

    @Test
    void testFindBySecretTypeOrderByNameAsc() {
        List<SecretEntity> apiSecrets = secretRepository.findBySecretTypeOrderByNameAsc("api");

        assertEquals(2, apiSecrets.size());
        assertEquals("api-token", apiSecrets.get(0).getName());
        assertEquals("legacy-key", apiSecrets.get(1).getName());
    }

    @Test
    void testFindByStatusOrderByNameAsc() {
        List<SecretEntity> activeSecrets = secretRepository.findByStatusOrderByNameAsc("active");

        assertEquals(2, activeSecrets.size());
        assertEquals("api-token", activeSecrets.get(0).getName());
        assertEquals("database-password", activeSecrets.get(1).getName());
    }

    @Test
    void testFindByStatusOrderByNameAscNoResults() {
        List<SecretEntity> rotatedSecrets = secretRepository.findByStatusOrderByNameAsc("rotated");

        assertTrue(rotatedSecrets.isEmpty());
    }
}
