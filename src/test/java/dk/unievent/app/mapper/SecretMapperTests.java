package dk.unievent.app.mapper;

import dk.unievent.app.application.dto.SecretDTO;
import dk.unievent.app.application.mapper.SecretMapper;
import dk.unievent.app.db.model.SecretEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SecretMapperTests {

    private SecretMapper secretMapper;

    @BeforeEach
    void setUp() {
        secretMapper = new SecretMapper();
    }

    @Test
    void testToDTOWithNull() {
        SecretDTO result = secretMapper.toDTO(null);

        assertNull(result);
    }

    @Test
    void testToDTOWithFullEntity() {
        LocalDateTime now = LocalDateTime.now();
        SecretEntity entity = SecretEntity.builder()
            .id(1L)
            .name("db-password")
            .secretType("database")
            .vaultPath("secret/data/app")
            .vaultKey("password")
            .status("active")
            .lastSyncedAt(now.minusHours(2))
            .expiresAt(now.plusDays(30))
            .createdAt(now.minusDays(5))
            .updatedAt(now.minusDays(1))
            .build();

        SecretDTO result = secretMapper.toDTO(entity);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("db-password", result.getName());
        assertEquals("database", result.getSecretType());
        assertEquals("secret/data/app", result.getVaultPath());
        assertEquals("password", result.getVaultKey());
        assertEquals("active", result.getStatus());
        assertEquals(entity.getLastSyncedAt(), result.getLastSyncedAt());
        assertEquals(entity.getExpiresAt(), result.getExpiresAt());
        assertEquals(entity.getCreatedAt(), result.getCreatedAt());
        assertEquals(entity.getUpdatedAt(), result.getUpdatedAt());
    }

    @Test
    void testToEntityWithNull() {
        SecretEntity result = secretMapper.toEntity(null);

        assertNull(result);
    }

    @Test
    void testToEntityWithFullDTO() {
        LocalDateTime now = LocalDateTime.now();
        SecretDTO dto = new SecretDTO(
            3L,
            "api-token",
            "api",
            "secret/data/ext",
            "token",
            "active",
            now.minusMinutes(15),
            now.plusDays(7),
            now.minusDays(10),
            now.minusDays(2)
        );

        SecretEntity result = secretMapper.toEntity(dto);

        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals("api-token", result.getName());
        assertEquals("api", result.getSecretType());
        assertEquals("secret/data/ext", result.getVaultPath());
        assertEquals("token", result.getVaultKey());
        assertEquals("active", result.getStatus());
        assertEquals(dto.getLastSyncedAt(), result.getLastSyncedAt());
        assertEquals(dto.getExpiresAt(), result.getExpiresAt());
        assertNull(result.getCreatedAt());
        assertNull(result.getUpdatedAt());
    }

    @Test
    void testEntityDtoRoundTripPreservesCoreFields() {
        LocalDateTime now = LocalDateTime.now();
        SecretEntity entity = SecretEntity.builder()
            .id(11L)
            .name("service-key")
            .secretType("service")
            .vaultPath("secret/data/service")
            .vaultKey("key")
            .status("inactive")
            .lastSyncedAt(now.minusDays(1))
            .expiresAt(now.plusDays(14))
            .createdAt(now.minusDays(20))
            .updatedAt(now.minusDays(2))
            .build();

        SecretDTO dto = secretMapper.toDTO(entity);
        SecretEntity remapped = secretMapper.toEntity(dto);

        assertNotNull(remapped);
        assertEquals(entity.getId(), remapped.getId());
        assertEquals(entity.getName(), remapped.getName());
        assertEquals(entity.getSecretType(), remapped.getSecretType());
        assertEquals(entity.getVaultPath(), remapped.getVaultPath());
        assertEquals(entity.getVaultKey(), remapped.getVaultKey());
        assertEquals(entity.getStatus(), remapped.getStatus());
        assertEquals(entity.getLastSyncedAt(), remapped.getLastSyncedAt());
        assertEquals(entity.getExpiresAt(), remapped.getExpiresAt());
    }
}
