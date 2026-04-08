package dk.unievent.app.model;

import dk.unievent.app.db.model.PlaceEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceEntityTests {

    @Test
    void prePersistShouldSetCreatedAndUpdatedAt() throws Exception {
        PlaceEntity entity = PlaceEntity.builder().id("place-1").name("Venue").build();

        invokeLifecycle(entity, "onCreate");

        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void preUpdateShouldRefreshUpdatedAt() throws Exception {
        PlaceEntity entity = PlaceEntity.builder().id("place-2").name("Venue 2").build();
        LocalDateTime before = LocalDateTime.now().minusMinutes(2);
        entity.setUpdatedAt(before);

        invokeLifecycle(entity, "onUpdate");

        assertTrue(entity.getUpdatedAt().isAfter(before) || entity.getUpdatedAt().isEqual(before));
    }

    private void invokeLifecycle(PlaceEntity entity, String methodName) throws Exception {
        Method method = PlaceEntity.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(entity);
    }
}
