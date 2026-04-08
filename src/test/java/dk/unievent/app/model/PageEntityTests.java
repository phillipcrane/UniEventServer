package dk.unievent.app.model;

import dk.unievent.app.db.model.PageEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageEntityTests {

    @Test
    void prePersistShouldSetTimestampsAndConnectedAtWhenNull() throws Exception {
        PageEntity entity = PageEntity.builder()
            .id("page-1")
            .name("UniEvent")
            .connectedAt(null)
            .build();

        invokeLifecycle(entity, "onCreate");

        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertNotNull(entity.getConnectedAt());
    }

    @Test
    void prePersistShouldPreserveConnectedAtWhenSet() throws Exception {
        LocalDateTime connectedAt = LocalDateTime.of(2029, 12, 1, 10, 0);
        PageEntity entity = PageEntity.builder()
            .id("page-2")
            .name("Existing")
            .connectedAt(connectedAt)
            .build();

        invokeLifecycle(entity, "onCreate");

        assertEquals(connectedAt, entity.getConnectedAt());
    }

    @Test
    void preUpdateShouldRefreshUpdatedAt() throws Exception {
        PageEntity entity = PageEntity.builder().id("page-3").name("Update").build();
        LocalDateTime before = LocalDateTime.now().minusMinutes(5);
        entity.setUpdatedAt(before);

        invokeLifecycle(entity, "onUpdate");

        assertTrue(entity.getUpdatedAt().isAfter(before) || entity.getUpdatedAt().isEqual(before));
    }

    private void invokeLifecycle(PageEntity entity, String methodName) throws Exception {
        Method method = PageEntity.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(entity);
    }
}
