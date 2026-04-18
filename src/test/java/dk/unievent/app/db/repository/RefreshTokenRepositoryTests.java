package dk.unievent.app.db.repository;

import dk.unievent.app.db.model.RefreshTokenEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RefreshTokenRepositoryTests {

    @Autowired
    private RefreshTokenRepository repository;

    private RefreshTokenEntity activeToken;

    @BeforeEach
    void setUp() {
        activeToken = repository.save(RefreshTokenEntity.builder()
            .tokenId("tok-active")
            .familyId("fam-1")
            .tokenHash("hash-active")
            .userId(1L)
            .userEmail("user@example.com")
            .expiresAt(Instant.now().plusSeconds(3600))
            .build());
    }

    @Test
    void findByTokenIdShouldReturnEntityWhenFound() {
        Optional<RefreshTokenEntity> result = repository.findByTokenId("tok-active");
        assertTrue(result.isPresent());
        assertEquals("fam-1", result.get().getFamilyId());
    }

    @Test
    void findByTokenIdShouldReturnEmptyWhenNotFound() {
        assertTrue(repository.findByTokenId("nonexistent").isEmpty());
    }

    @Test
    void findAllByFamilyIdAndRevokedAtIsNullShouldReturnActiveTokens() {
        repository.save(RefreshTokenEntity.builder()
            .tokenId("tok-revoked")
            .familyId("fam-1")
            .tokenHash("hash-revoked")
            .userId(1L)
            .userEmail("user@example.com")
            .expiresAt(Instant.now().plusSeconds(3600))
            .revokedAt(Instant.now())
            .build());

        List<RefreshTokenEntity> result = repository.findAllByFamilyIdAndRevokedAtIsNull("fam-1");

        assertEquals(1, result.size());
        assertEquals("tok-active", result.get(0).getTokenId());
    }

    @Test
    void findAllByUserEmailAndRevokedAtIsNullShouldReturnActiveTokens() {
        List<RefreshTokenEntity> result = repository.findAllByUserEmailAndRevokedAtIsNull("user@example.com");

        assertEquals(1, result.size());
        assertEquals("tok-active", result.get(0).getTokenId());
    }

    @Test
    void findAllByExpiresAtBeforeShouldReturnExpiredTokens() {
        repository.save(RefreshTokenEntity.builder()
            .tokenId("tok-expired")
            .familyId("fam-2")
            .tokenHash("hash-expired")
            .userId(2L)
            .userEmail("other@example.com")
            .expiresAt(Instant.now().minusSeconds(60))
            .build());

        List<RefreshTokenEntity> expired = repository.findAllByExpiresAtBefore(Instant.now());

        assertEquals(1, expired.size());
        assertEquals("tok-expired", expired.get(0).getTokenId());
    }

    @Test
    void saveShouldSetTimestamps() {
        assertNotNull(activeToken.getCreatedAt());
        assertNotNull(activeToken.getUpdatedAt());
    }
}
