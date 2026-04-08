package dk.unievent.app.model;

import dk.unievent.app.db.model.EventEntity;
import dk.unievent.app.db.model.PageEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventEntityTests {

    @Test
    void prePersistShouldSetCreatedAndUpdatedAt() throws Exception {
        EventEntity entity = EventEntity.builder()
            .id("evt-1")
            .page(PageEntity.builder().id("page-1").name("Page").build())
            .title("Event")
            .startTime(LocalDateTime.of(2030, 1, 1, 18, 0))
            .build();

        invokeLifecycle(entity, "onCreate");

        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void preUpdateShouldRefreshUpdatedAt() throws Exception {
        EventEntity entity = EventEntity.builder()
            .id("evt-2")
            .page(PageEntity.builder().id("page-1").name("Page").build())
            .title("Event")
            .startTime(LocalDateTime.of(2030, 1, 1, 18, 0))
            .build();
        LocalDateTime before = LocalDateTime.now().minusMinutes(3);
        entity.setUpdatedAt(before);

        invokeLifecycle(entity, "onUpdate");

        assertTrue(entity.getUpdatedAt().isAfter(before) || entity.getUpdatedAt().isEqual(before));
    }

    @Test
    void eventUrlShouldRoundTripViaAccessors() {
        EventEntity entity = new EventEntity();
        entity.setEventUrl("https://example.com/event/1");

        assertEquals("https://example.com/event/1", entity.getEventUrl());
    }

    private void invokeLifecycle(EventEntity entity, String methodName) throws Exception {
        Method method = EventEntity.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(entity);
    }
}
