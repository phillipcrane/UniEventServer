package dk.unievent.app.db.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenEntityTests {

    @Test
    void prePersistShouldSetBothTimestamps() throws Exception {
        RefreshTokenEntity entity = RefreshTokenEntity.builder()
            .tokenId("tok-1")
            .familyId("fam-1")
            .tokenHash("hash")
            .userId(1L)
            .userEmail("user@example.com")
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        invokeLifecycle(entity, "onCreate");

        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertFalse(entity.getCreatedAt().isAfter(Instant.now()));
    }

    @Test
    void preUpdateShouldRefreshUpdatedAtButNotCreatedAt() throws Exception {
        RefreshTokenEntity entity = RefreshTokenEntity.builder()
            .tokenId("tok-2")
            .familyId("fam-1")
            .tokenHash("hash")
            .userId(1L)
            .userEmail("user@example.com")
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        invokeLifecycle(entity, "onCreate");
        Instant createdAt = entity.getCreatedAt();

        Thread.sleep(5);
        invokeLifecycle(entity, "onUpdate");

        assertEquals(createdAt, entity.getCreatedAt());
        assertFalse(entity.getUpdatedAt().isBefore(createdAt));
    }

    private void invokeLifecycle(RefreshTokenEntity entity, String methodName) throws Exception {
        Method method = RefreshTokenEntity.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(entity);
    }
}
