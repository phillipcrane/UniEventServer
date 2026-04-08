package dk.unievent.app.mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.unievent.app.application.dto.EventDTO;
import dk.unievent.app.application.dto.PlaceDTO;
import dk.unievent.app.application.mapper.EventMapper;
import dk.unievent.app.application.mapper.PlaceMapper;
import dk.unievent.app.db.model.EventEntity;
import dk.unievent.app.db.model.MediaEntity;
import dk.unievent.app.db.model.PageEntity;
import dk.unievent.app.db.model.PlaceEntity;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EventMapper
 * Tests conversion between EventEntity ↔ EventDTO
 */
@ExtendWith(MockitoExtension.class)
class EventMapperTests {
    
    @Mock
    private PlaceMapper placeMapper;
    
    @InjectMocks
    private EventMapper eventMapper;
    
    private EventEntity testEventEntity;
    private PageEntity testPageEntity;
    private PlaceEntity testPlaceEntity;
    private MediaEntity testCoverImage;
    
    @BeforeEach
    void setUp() {
        testPageEntity = PageEntity.builder()
                .id("page-1")
                .name("Test Page")
                .build();
        
        testPlaceEntity = PlaceEntity.builder()
                .id("place-1")
                .name("Test Venue")
                .city("Copenhagen")
                .country("Denmark")
                .build();
        
        testCoverImage = MediaEntity.builder()
                .id(1L)
                .filename("cover.jpg")
                .contentType("image/jpeg")
                .fileId("3,abc123")
                .uploadedAt(Instant.now())
                .build();
        
        testEventEntity = EventEntity.builder()
                .id("event-1")
                .title("Test Event")
                .description("Test Description")
                .startTime(LocalDateTime.of(2026, 3, 20, 18, 0))
                .endTime(LocalDateTime.of(2026, 3, 20, 20, 0))
                .coverImage(testCoverImage)
                .eventUrl("https://example.com/event")
                .page(testPageEntity)
                .place(testPlaceEntity)
                .createdAt(LocalDateTime.of(2026, 3, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 10, 15, 30))
                .build();
    }
    
    // ============= toDTO Tests =============
    
    @Test
    void testToDTOWithNull() {
        EventDTO result = eventMapper.toDTO(null);
        
        assertNull(result);
    }
    
    @Test
    void testToDTOWithFullEntity() {
        PlaceDTO placeDTOResult = new PlaceDTO();
        placeDTOResult.setId("place-1");
        placeDTOResult.setName("Test Venue");
        when(placeMapper.toDTO(testPlaceEntity)).thenReturn(placeDTOResult);
        
        EventDTO result = eventMapper.toDTO(testEventEntity);
        
        assertNotNull(result);
        assertEquals("event-1", result.getId());
        assertEquals("Test Event", result.getTitle());
        assertEquals("Test Description", result.getDescription());
        assertEquals(LocalDateTime.of(2026, 3, 20, 18, 0), result.getStartTime());
        assertEquals(LocalDateTime.of(2026, 3, 20, 20, 0), result.getEndTime());
        assertEquals(1L, result.getCoverImageId());
        assertEquals("https://example.com/event", result.getEventUrl());
        assertEquals("page-1", result.getPageId());
        assertEquals(placeDTOResult, result.getPlace());
        assertEquals(LocalDateTime.of(2026, 3, 1, 10, 0), result.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 3, 10, 15, 30), result.getUpdatedAt());
    }
    
    @Test
    void testToDTOWithNullPage() {
        testEventEntity.setPage(null);
        PlaceDTO placeDTOResult = new PlaceDTO();
        placeDTOResult.setId("place-1");
        when(placeMapper.toDTO(testPlaceEntity)).thenReturn(placeDTOResult);
        
        EventDTO result = eventMapper.toDTO(testEventEntity);
        
        assertNotNull(result);
        assertNull(result.getPageId());
        assertEquals("event-1", result.getId());
    }
    
    @Test
    void testToDTOWithNullPlace() {
        testEventEntity.setPlace(null);
        when(placeMapper.toDTO(null)).thenReturn(null);
        
        EventDTO result = eventMapper.toDTO(testEventEntity);
        
        assertNotNull(result);
        assertNull(result.getPlace());
        assertEquals("event-1", result.getId());
        assertEquals("page-1", result.getPageId());
    }
    
    @Test
    void testToDTOWithNullPageAndPlace() {
        testEventEntity.setPage(null);
        testEventEntity.setPlace(null);
        when(placeMapper.toDTO(null)).thenReturn(null);
        
        EventDTO result = eventMapper.toDTO(testEventEntity);
        
        assertNotNull(result);
        assertNull(result.getPageId());
        assertNull(result.getPlace());
    }
    
    // ============= toEntity Tests =============
    
    @Test
    void testToEntityWithNull() {
        EventEntity result = eventMapper.toEntity(null);
        
        assertNull(result);
    }
    
    @Test
    void testToEntityWithFullDTO() {
        EventDTO dto = new EventDTO();
        dto.setId("event-new");
        dto.setTitle("New Event");
        dto.setDescription("New Description");
        dto.setStartTime(LocalDateTime.of(2026, 4, 1, 19, 0));
        dto.setEndTime(LocalDateTime.of(2026, 4, 1, 21, 0));
        dto.setCoverImageId(1L);
        dto.setEventUrl("https://example.com/event-new");
        dto.setPageId("page-new");
        
        EventEntity result = eventMapper.toEntity(dto);
        
        assertNotNull(result);
        assertEquals("event-new", result.getId());
        assertEquals("New Event", result.getTitle());
        assertEquals("New Description", result.getDescription());
        assertEquals(LocalDateTime.of(2026, 4, 1, 19, 0), result.getStartTime());
        assertEquals(LocalDateTime.of(2026, 4, 1, 21, 0), result.getEndTime());
        assertEquals("https://example.com/event-new", result.getEventUrl());
        assertNull(result.getCoverImage());
        assertNull(result.getPage());
    }
    
    @Test
    void testToEntityWithMinimalDTO() {
        EventDTO dto = new EventDTO();
        dto.setId("event-minimal");
        dto.setTitle("Minimal Event");
        
        EventEntity result = eventMapper.toEntity(dto);
        
        assertNotNull(result);
        assertEquals("event-minimal", result.getId());
        assertEquals("Minimal Event", result.getTitle());
        assertNull(result.getDescription());
        assertNull(result.getStartTime());
        assertNull(result.getEndTime());
    }
}

