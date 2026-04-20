package dk.unievent.app.db.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class UserEntityTests {

    @Test
    void prePersistShouldSetBothTimestamps() throws Exception {
        UserEntity entity = UserEntity.builder()
            .username("testuser")
            .email("test@example.com")
            .password("encoded")
            .build();

        invokeLifecycle(entity, "onCreate");

        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertFalse(entity.getCreatedAt().isAfter(Instant.now()));
        assertFalse(entity.getUpdatedAt().isAfter(Instant.now()));
    }

    @Test
    void preUpdateShouldRefreshUpdatedAt() throws Exception {
        UserEntity entity = UserEntity.builder()
            .username("testuser")
            .email("test@example.com")
            .password("encoded")
            .build();

        invokeLifecycle(entity, "onCreate");
        Instant createdAt = entity.getCreatedAt();

        Thread.sleep(5);
        invokeLifecycle(entity, "onUpdate");

        assertEquals(createdAt, entity.getCreatedAt());
        assertFalse(entity.getUpdatedAt().isBefore(createdAt));
    }

    @Test
    void builderDefaultRoleShouldBeUser() {
        UserEntity entity = UserEntity.builder()
            .username("testuser")
            .email("test@example.com")
            .password("encoded")
            .build();

        assertEquals("user", entity.getRole());
    }

    @Test
    void builderShouldAllowOverridingRole() {
        UserEntity entity = UserEntity.builder()
            .username("admin")
            .email("admin@example.com")
            .password("encoded")
            .role("admin")
            .build();

        assertEquals("ADMIN", entity.getRole());
    }

    private void invokeLifecycle(UserEntity entity, String methodName) throws Exception {
        Method method = UserEntity.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(entity);
    }
}
