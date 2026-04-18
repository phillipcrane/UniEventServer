package dk.unievent.app.application.mapper;

import dk.unievent.app.api.dto.FbEventResponse;
import dk.unievent.app.api.dto.FbPlaceData;
import dk.unievent.app.application.service.PlaceService;
import dk.unievent.app.db.model.EventEntity;
import dk.unievent.app.db.model.PlaceEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacebookEventMapperTests {

    @Mock
    private PlaceService placeService;

    @InjectMocks
    private FacebookEventMapper mapper;

    @Test
    void shouldMapBasicFieldsFromFbEvent() {
        FbEventResponse fbEvent = new FbEventResponse();
        fbEvent.setId("123456");
        fbEvent.setName("Tech Meetup");
        fbEvent.setDescription("A great event");
        fbEvent.setStartTime(OffsetDateTime.of(2026, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        fbEvent.setEndTime(OffsetDateTime.of(2026, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC));

        EventEntity result = mapper.mapToEventEntity("page-1", fbEvent);

        assertEquals("123456", result.getId());
        assertEquals("Tech Meetup", result.getTitle());
        assertEquals("A great event", result.getDescription());
        assertEquals("https://facebook.com/events/123456", result.getEventUrl());
        assertNotNull(result.getStartTime());
        assertNotNull(result.getEndTime());
    }

    @Test
    void shouldConvertOffsetDateTimeToLocalDateTime() {
        FbEventResponse fbEvent = new FbEventResponse();
        fbEvent.setId("abc");
        fbEvent.setStartTime(OffsetDateTime.of(2026, 6, 1, 14, 30, 0, 0, ZoneOffset.ofHours(2)));
        fbEvent.setEndTime(OffsetDateTime.of(2026, 6, 1, 16, 0, 0, 0, ZoneOffset.ofHours(2)));

        EventEntity result = mapper.mapToEventEntity("page-1", fbEvent);

        assertEquals(14, result.getStartTime().getHour());
        assertEquals(30, result.getStartTime().getMinute());
        assertEquals(16, result.getEndTime().getHour());
    }

    @Test
    void shouldSetPlaceWhenFbEventHasPlace() {
        FbPlaceData place = new FbPlaceData("The Venue", "Main St", "Copenhagen", "1000", "Denmark");
        FbEventResponse fbEvent = new FbEventResponse();
        fbEvent.setId("evt1");
        fbEvent.setPlace(place);

        PlaceEntity mockPlace = PlaceEntity.builder().id("place-uuid").name("The Venue").build();
        when(placeService.createOrFindPlace("The Venue", "Main St", "Copenhagen", "1000", "Denmark"))
            .thenReturn(mockPlace);

        EventEntity result = mapper.mapToEventEntity("page-1", fbEvent);

        assertNotNull(result.getPlace());
        assertEquals("place-uuid", result.getPlace().getId());
    }

    @Test
    void shouldNotSetPlaceWhenFbEventHasNoPlace() {
        FbEventResponse fbEvent = new FbEventResponse();
        fbEvent.setId("evt2");
        fbEvent.setPlace(null);

        EventEntity result = mapper.mapToEventEntity("page-1", fbEvent);

        assertNull(result.getPlace());
        verifyNoInteractions(placeService);
    }

    @Test
    void shouldNotSetPlaceWhenPlaceNameIsNull() {
        FbPlaceData place = new FbPlaceData(null, "Street", "City", "1000", "Country");
        FbEventResponse fbEvent = new FbEventResponse();
        fbEvent.setId("evt3");
        fbEvent.setPlace(place);

        EventEntity result = mapper.mapToEventEntity("page-1", fbEvent);

        assertNull(result.getPlace());
        verifyNoInteractions(placeService);
    }

    @Test
    void shouldHandleNullStartAndEndTime() {
        FbEventResponse fbEvent = new FbEventResponse();
        fbEvent.setId("evt4");
        fbEvent.setStartTime(null);
        fbEvent.setEndTime(null);

        EventEntity result = mapper.mapToEventEntity("page-1", fbEvent);

        assertNull(result.getStartTime());
        assertNull(result.getEndTime());
    }
}
