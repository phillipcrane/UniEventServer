package dk.unievent.app.dto;

import dk.unievent.app.application.dto.SecretDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecretDTOTests {

    @Test
    void allArgsConstructorShouldPopulateFields() {
        LocalDateTime now = LocalDateTime.now();

        SecretDTO dto = new SecretDTO(
            1L,
            "db-password",
            "database",
            "secret/data/db",
            "password",
            "active",
            now.minusHours(1),
            now.plusDays(30),
            now.minusDays(10),
            now.minusDays(1)
        );

        assertEquals(1L, dto.getId());
        assertEquals("db-password", dto.getName());
        assertEquals("database", dto.getSecretType());
        assertEquals("secret/data/db", dto.getVaultPath());
        assertEquals("password", dto.getVaultKey());
        assertEquals("active", dto.getStatus());
        assertEquals(now.plusDays(30), dto.getExpiresAt());
    }
}
