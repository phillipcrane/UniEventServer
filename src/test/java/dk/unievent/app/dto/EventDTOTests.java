package dk.unievent.app.dto;

import dk.unievent.app.application.dto.EventDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventDTOTests {

    @Test
    void gettersAndSettersShouldStoreEventFields() {
        EventDTO dto = new EventDTO();
        LocalDateTime start = LocalDateTime.of(2030, 1, 1, 18, 0);
        LocalDateTime end = LocalDateTime.of(2030, 1, 1, 20, 0);

        dto.setId("evt-1");
        dto.setPageId("page-1");
        dto.setTitle("Sample Event");
        dto.setDescription("Description");
        dto.setStartTime(start);
        dto.setEndTime(end);
        dto.setCoverImageId(99L);
        dto.setEventUrl("https://example.com/events/1");

        assertEquals("evt-1", dto.getId());
        assertEquals("page-1", dto.getPageId());
        assertEquals("Sample Event", dto.getTitle());
        assertEquals("Description", dto.getDescription());
        assertEquals(start, dto.getStartTime());
        assertEquals(end, dto.getEndTime());
        assertEquals(99L, dto.getCoverImageId());
        assertEquals("https://example.com/events/1", dto.getEventUrl());
    }
}
