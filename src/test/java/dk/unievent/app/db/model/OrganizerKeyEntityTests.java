package dk.unievent.app.db.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class OrganizerKeyEntityTests {

    @Test
    void prePersistShouldSetBothTimestamps() throws Exception {
        OrganizerKeyEntity entity = buildKey();
        invokeLifecycle(entity, "onCreate");

        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertFalse(entity.getCreatedAt().isAfter(Instant.now()));
        assertFalse(entity.getUpdatedAt().isAfter(Instant.now()));
    }

    @Test
    void preUpdateShouldRefreshUpdatedAtAndNotCreatedAt() throws Exception {
        OrganizerKeyEntity entity = buildKey();
        invokeLifecycle(entity, "onCreate");
        Instant createdAt = entity.getCreatedAt();

        Thread.sleep(5);
        invokeLifecycle(entity, "onUpdate");

        assertEquals(createdAt, entity.getCreatedAt());
        assertFalse(entity.getUpdatedAt().isBefore(createdAt));
    }

    @Test
    void builderShouldStoreAllFields() {
        Instant expiresAt = Instant.now().plusSeconds(86400);
        OrganizerKeyEntity entity = OrganizerKeyEntity.builder()
            .keyValue("ABCDEFGHIJKLMNOPQRSTUVWXYZ123456")
            .email("organizer@example.com")
            .createdBy(42L)
            .expiresAt(expiresAt)
            .build();

        assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ123456", entity.getKeyValue());
        assertEquals("organizer@example.com", entity.getEmail());
        assertEquals(42L, entity.getCreatedBy());
        assertEquals(expiresAt, entity.getExpiresAt());
    }

    @Test
    void usedAtShouldDefaultToNull() {
        assertNull(buildKey().getUsedAt());
    }

    private OrganizerKeyEntity buildKey() {
        return OrganizerKeyEntity.builder()
            .keyValue("ABCDEFGHIJKLMNOPQRSTUVWXYZ123456")
            .email("organizer@example.com")
            .createdBy(1L)
            .expiresAt(Instant.now().plusSeconds(86400))
            .build();
    }

    private void invokeLifecycle(OrganizerKeyEntity entity, String methodName) throws Exception {
        Method method = OrganizerKeyEntity.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(entity);
    }
}
