package dk.unievent.app.model;

import dk.unievent.app.db.model.SecretEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretEntityTests {

    @Test
    void prePersistShouldSetTimestampsAndDefaultStatusWhenMissing() throws Exception {
        SecretEntity entity = SecretEntity.builder()
            .name("db-password")
            .secretType("database")
            .vaultPath("secret/data/db")
            .status(null)
            .build();

        invokeLifecycle(entity, "onCreate");

        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertEquals("active", entity.getStatus());
    }

    @Test
    void prePersistShouldNotOverrideExistingStatus() throws Exception {
        SecretEntity entity = SecretEntity.builder()
            .name("legacy-token")
            .secretType("api")
            .vaultPath("secret/data/api")
            .status("inactive")
            .build();

        invokeLifecycle(entity, "onCreate");

        assertEquals("inactive", entity.getStatus());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void preUpdateShouldRefreshUpdatedAt() throws Exception {
        SecretEntity entity = SecretEntity.builder()
            .name("service-key")
            .secretType("service")
            .vaultPath("secret/data/service")
            .status("active")
            .build();

        LocalDateTime before = LocalDateTime.now().minusMinutes(2);
        entity.setUpdatedAt(before);

        invokeLifecycle(entity, "onUpdate");

        assertNotNull(entity.getUpdatedAt());
        assertTrue(entity.getUpdatedAt().isAfter(before) || entity.getUpdatedAt().isEqual(before));
    }

    private void invokeLifecycle(SecretEntity entity, String methodName) throws Exception {
        Method method = SecretEntity.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(entity);
    }
}
