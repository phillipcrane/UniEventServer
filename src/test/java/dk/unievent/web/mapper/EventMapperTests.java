package dk.unievent.web.mapper;

import dk.unievent.web.dto.EventDTO;
import dk.unievent.web.dto.PlaceDTO;
import dk.unievent.web.model.EventEntity;
import dk.unievent.web.model.PageEntity;
import dk.unievent.web.model.PlaceEntity;
import dk.unievent.web.repository.PageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    
    @Mock
    private PageRepository pageRepository;
    
    @InjectMocks
    private EventMapper eventMapper;
    
    private EventEntity testEventEntity;
    private PageEntity testPageEntity;
    private PlaceEntity testPlaceEntity;
    
    @BeforeEach
    void setUp() {
        testPageEntity = new PageEntity();
        testPageEntity.setId("page-1");
        testPageEntity.setName("Test Page");
        
        testPlaceEntity = new PlaceEntity();
        testPlaceEntity.setId("place-1");
        testPlaceEntity.setName("Test Venue");
        testPlaceEntity.setCity("Copenhagen");
        testPlaceEntity.setCountry("Denmark");
        
        testEventEntity = new EventEntity();
        testEventEntity.setId("event-1");
        testEventEntity.setTitle("Test Event");
        testEventEntity.setDescription("Test Description");
        testEventEntity.setStartTime(LocalDateTime.of(2026, 3, 20, 18, 0));
        testEventEntity.setEndTime(LocalDateTime.of(2026, 3, 20, 20, 0));
        // coverImage mapping is handled through the MediaEntity relationship
        // No direct ID setting needed as the mapper handles the conversion
        testEventEntity.setEventURL("https://example.com/event");
        testEventEntity.setPage(testPageEntity);
        testEventEntity.setPlace(testPlaceEntity);
        testEventEntity.setCreatedAt(LocalDateTime.of(2026, 3, 1, 10, 0));
        testEventEntity.setUpdatedAt(LocalDateTime.of(2026, 3, 10, 15, 30));
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
        assertEquals("https://example.com/event", result.getEventURL());
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
        dto.setEventURL("https://example.com/event-new");
        dto.setPageId("page-new");
        
        EventEntity result = eventMapper.toEntity(dto);
        
        assertNotNull(result);
        assertEquals("event-new", result.getId());
        assertEquals("New Event", result.getTitle());
        assertEquals("New Description", result.getDescription());
        assertEquals(LocalDateTime.of(2026, 4, 1, 19, 0), result.getStartTime());
        assertEquals(LocalDateTime.of(2026, 4, 1, 21, 0), result.getEndTime());
        // coverImage mapping is handled through mapper
        assertNull(result.getCoverImage());
        assertEquals("https://example.com/event-new", result.getEventURL());
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
