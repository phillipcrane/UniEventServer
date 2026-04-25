package dk.unievent.app.db.repository;

import dk.unievent.app.db.model.OrganizerKeyEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrganizerKeyRepositoryTests {

    @Autowired
    private OrganizerKeyRepository organizerKeyRepository;

    private OrganizerKeyEntity activeKey;

    @BeforeEach
    void setUp() {
        activeKey = organizerKeyRepository.save(OrganizerKeyEntity.builder()
            .keyValue(key("ACTIVE"))
            .email("organizer@example.com")
            .createdBy(1L)
            .expiresAt(Instant.now().plusSeconds(86400))
            .build());
    }

    @Test
    void saveShouldSetTimestamps() {
        assertNotNull(activeKey.getCreatedAt());
        assertNotNull(activeKey.getUpdatedAt());
    }

    @Test
    void findByKeyValueShouldReturnKeyWhenExists() {
        Optional<OrganizerKeyEntity> found = organizerKeyRepository.findByKeyValue(key("ACTIVE"));

        assertTrue(found.isPresent());
        assertEquals("organizer@example.com", found.get().getEmail());
    }

    @Test
    void findByKeyValueShouldReturnEmptyWhenNotFound() {
        Optional<OrganizerKeyEntity> found = organizerKeyRepository.findByKeyValue(key("MISSING"));

        assertTrue(found.isEmpty());
    }

    @Test
    void findByEmailAndUsedAtIsNullShouldReturnUnusedKeys() {
        List<OrganizerKeyEntity> keys = organizerKeyRepository.findByEmailAndUsedAtIsNull("organizer@example.com");

        assertEquals(1, keys.size());
        assertNull(keys.get(0).getUsedAt());
    }

    @Test
    void findByEmailAndUsedAtIsNullShouldExcludeUsedKeys() {
        activeKey.setUsedAt(Instant.now());
        organizerKeyRepository.save(activeKey);

        List<OrganizerKeyEntity> keys = organizerKeyRepository.findByEmailAndUsedAtIsNull("organizer@example.com");

        assertTrue(keys.isEmpty());
    }

    @Test
    void findByExpiresAtBeforeAndUsedAtIsNullShouldReturnExpiredUnusedKeys() {
        OrganizerKeyEntity expiredKey = organizerKeyRepository.save(OrganizerKeyEntity.builder()
            .keyValue(key("EXPIRED"))
            .email("expired@example.com")
            .createdBy(1L)
            .expiresAt(Instant.now().minusSeconds(3600))
            .build());

        List<OrganizerKeyEntity> expired = organizerKeyRepository.findByExpiresAtBeforeAndUsedAtIsNull(Instant.now());

        assertTrue(expired.stream().anyMatch(k -> k.getId().equals(expiredKey.getId())));
    }

    @Test
    void findByExpiresAtBeforeAndUsedAtIsNullShouldExcludeUsedExpiredKeys() {
        OrganizerKeyEntity usedExpired = organizerKeyRepository.save(OrganizerKeyEntity.builder()
            .keyValue(key("USEDEXPD"))
            .email("usedexpired@example.com")
            .createdBy(1L)
            .expiresAt(Instant.now().minusSeconds(3600))
            .usedAt(Instant.now().minusSeconds(1800))
            .build());

        List<OrganizerKeyEntity> expired = organizerKeyRepository.findByExpiresAtBeforeAndUsedAtIsNull(Instant.now());

        assertTrue(expired.stream().noneMatch(k -> k.getId().equals(usedExpired.getId())));
    }

    @Test
    void findByExpiresAtBeforeAndUsedAtIsNullShouldExcludeActiveKeys() {
        List<OrganizerKeyEntity> expired = organizerKeyRepository.findByExpiresAtBeforeAndUsedAtIsNull(Instant.now());

        assertTrue(expired.stream().noneMatch(k -> k.getId().equals(activeKey.getId())));
    }

    @Test
    void saveShouldThrowOnDuplicateKeyValue() {
        OrganizerKeyEntity duplicate = OrganizerKeyEntity.builder()
            .keyValue(key("ACTIVE"))
            .email("other@example.com")
            .createdBy(1L)
            .expiresAt(Instant.now().plusSeconds(86400))
            .build();

        assertThrows(DataIntegrityViolationException.class,
            () -> organizerKeyRepository.saveAndFlush(duplicate));
    }

    private static String key(String prefix) {
        return (prefix + "0".repeat(32)).substring(0, 32);
    }
}
