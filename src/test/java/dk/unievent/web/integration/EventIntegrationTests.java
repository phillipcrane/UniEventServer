package dk.unievent.web.integration;

import dk.unievent.web.dto.EventDTO;
import dk.unievent.web.dto.PlaceDTO;
import dk.unievent.web.model.EventEntity;
import dk.unievent.web.model.PageEntity;
import dk.unievent.web.model.PlaceEntity;
import dk.unievent.web.repository.EventRepository;
import dk.unievent.web.repository.PageRepository;
import dk.unievent.web.repository.PlaceRepository;
import dk.unievent.web.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Event operations
 * Tests full stack: Database ↔ Repository ↔ Service ↔ Mapper ↔ DTO
 * Uses real H2 in-memory database and @Transactional for test isolation
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventIntegrationTests {
    
    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private PageRepository pageRepository;
    
    @Autowired
    private PlaceRepository placeRepository;
    
    @Autowired
    private EventService eventService;
    
    private PageEntity testPage;
    private PlaceEntity testPlace;
    
    @BeforeEach
    void setUp() {
        // Create test page
        testPage = PageEntity.builder()
                .id("page-1")
                .name("S-huset")
                .tokenStatus("valid")
                .build();
        pageRepository.save(testPage);
        
        // Create test place
        testPlace = PlaceEntity.builder()
                .id("place-1")
                .name("Venue A")
                .city("Copenhagen")
                .country("Denmark")
                .build();
        placeRepository.save(testPlace);
    }
    
    // ============= Save/Create Tests =============
    
    @Test
    void testCreateEventAndRetrieveAsDTO() {
        EventDTO createDTO = new EventDTO();
        createDTO.setId("event-create-1");
        createDTO.setTitle("Workshop");
        createDTO.setDescription("Java Workshop");
        createDTO.setStartTime(LocalDateTime.now().plusDays(5));
        createDTO.setPageId(testPage.getId());
        
        EventDTO created = eventService.createEvent(createDTO);
        
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("Workshop", created.getTitle());
        assertEquals("Java Workshop", created.getDescription());
        assertEquals(testPage.getId(), created.getPageId());
    }
    
    @Test
    void testEventPersistenceInDatabase() {
        EventDTO createDTO = new EventDTO();
        createDTO.setId("event-persist-1");
        createDTO.setTitle("Concert");
        createDTO.setStartTime(LocalDateTime.of(2026, 4, 15, 20, 0));
        createDTO.setPageId(testPage.getId());
        
        EventDTO created = eventService.createEvent(createDTO);
        
        // Verify in database
        Optional<EventEntity> dbEntity = eventRepository.findById(created.getId());
        assertTrue(dbEntity.isPresent());
        assertEquals("Concert", dbEntity.get().getTitle());
        assertEquals(testPage.getId(), dbEntity.get().getPage().getId());
    }
    
    // ============= Retrieve Tests =============
    
    @Test
    void testGetAllEventsThroughService() {
        // Create multiple events
        EventDTO event1 = createTestEvent("Event 1", LocalDateTime.of(2026, 3, 20, 10, 0), testPage.getId());
        EventDTO event2 = createTestEvent("Event 2", LocalDateTime.of(2026, 3, 25, 14, 0), testPage.getId());
        EventDTO event3 = createTestEvent("Event 3", LocalDateTime.of(2026, 3, 15, 18, 0), testPage.getId());
        
        List<EventDTO> allEvents = eventService.getAllEvents();
        
        assertEquals(3, allEvents.size());
        // Verify ordering by startTime
        assertEquals(event3.getId(), allEvents.get(0).getId()); // 15th
        assertEquals(event1.getId(), allEvents.get(1).getId()); // 20th
        assertEquals(event2.getId(), allEvents.get(2).getId()); // 25th
    }
    
    @Test
    void testGetFutureEventsThroughService() {
        LocalDateTime now = LocalDateTime.now();
        
        // Create past and future events
        createTestEvent("Past Event", now.minusDays(1), testPage.getId());
        createTestEvent("Future Event 1", now.plusDays(3), testPage.getId());
        createTestEvent("Future Event 2", now.plusDays(7), testPage.getId());
        
        List<EventDTO> futureEvents = eventService.getFutureEvents();
        
        assertEquals(2, futureEvents.size());
        assertTrue(futureEvents.stream().allMatch(e -> e.getStartTime().isAfter(now)));
    }
    
    @Test
    void testGetEventByIdThroughService() {
        EventDTO created = createTestEvent("Find Me", LocalDateTime.now().plusDays(1), testPage.getId());
        
        EventDTO found = eventService.getEventById(created.getId());
        
        assertNotNull(found);
        assertEquals("Find Me", found.getTitle());
        assertEquals(created.getId(), found.getId());
    }
    
    @Test
    void testGetEventByIdNotFoundReturnsNull() {
        EventDTO notFound = eventService.getEventById("nonexistent");
        
        assertNull(notFound);
    }
    
    @Test
    void testGetEventsByPageId() {
        // Create page 2
        PageEntity page2 = PageEntity.builder()
                .id("page-2")
                .name("Other Page")
                .build();
        pageRepository.save(page2);
        
        // Create events for both pages
        createTestEvent("Event Page1-1", LocalDateTime.now().plusDays(1), testPage.getId());
        createTestEvent("Event Page1-2", LocalDateTime.now().plusDays(2), testPage.getId());
        createTestEvent("Event Page2-1", LocalDateTime.now().plusDays(3), page2.getId());
        
        List<EventDTO> page1Events = eventService.getEventsByPageId(testPage.getId());
        List<EventDTO> page2Events = eventService.getEventsByPageId(page2.getId());
        
        assertEquals(2, page1Events.size());
        assertEquals(1, page2Events.size());
        assertTrue(page1Events.stream().allMatch(e -> e.getPageId().equals(testPage.getId())));
        assertTrue(page2Events.stream().allMatch(e -> e.getPageId().equals(page2.getId())));
    }
    
    @Test
    void testGetEventsByPlaceId() {
        // Create place 2
        PlaceEntity place2 = PlaceEntity.builder()
                .id("place-2")
                .name("Venue B")
                .city("Aarhus")
                .country("Denmark")
                .build();
        placeRepository.save(place2);
        
        // Create events for different places
        createTestEventWithPlace("Event Place1", LocalDateTime.now().plusDays(1), testPlace.getId());
        createTestEventWithPlace("Event Place2", LocalDateTime.now().plusDays(2), place2.getId());
        
        List<EventDTO> place1Events = eventService.getEventsByPlaceId(testPlace.getId());
        List<EventDTO> place2Events = eventService.getEventsByPlaceId(place2.getId());
        
        assertEquals(1, place1Events.size());
        assertEquals(1, place2Events.size());
    }
    
    // ============= Update Tests =============
    
    @Test
    void testUpdateEventThroughService() {
        EventDTO created = createTestEvent("Original Title", LocalDateTime.now().plusDays(1), testPage.getId());
        
        EventDTO updateDTO = new EventDTO();
        updateDTO.setTitle("Updated Title");
        updateDTO.setDescription("Updated Desc");
        
        EventDTO updated = eventService.updateEvent(created.getId(), updateDTO);
        
        assertNotNull(updated);
        assertEquals("Updated Title", updated.getTitle());
        assertEquals("Updated Desc", updated.getDescription());
        assertEquals(created.getId(), updated.getId());
    }
    
    @Test
    void testUpdateEventNotFoundReturnsNull() {
        EventDTO updateDTO = new EventDTO();
        updateDTO.setTitle("New Title");
        
        EventDTO result = eventService.updateEvent("nonexistent", updateDTO);
        
        assertNull(result);
    }
    
    @Test
    void testUpdateEventPersistsToDatabase() {
        EventDTO created = createTestEvent("Original", LocalDateTime.now().plusDays(1), testPage.getId());
        
        EventDTO updateDTO = new EventDTO();
        updateDTO.setTitle("Persisted Title");
        
        eventService.updateEvent(created.getId(), updateDTO);
        
        // Verify in database
        Optional<EventEntity> dbEntity = eventRepository.findById(created.getId());
        assertTrue(dbEntity.isPresent());
        assertEquals("Persisted Title", dbEntity.get().getTitle());
    }
    
    // ============= Delete Tests =============
    
    @Test
    void testDeleteEventThroughService() {
        EventDTO created = createTestEvent("Delete Me", LocalDateTime.now().plusDays(1), testPage.getId());
        
        boolean deleted = eventService.deleteEvent(created.getId());
        
        assertTrue(deleted);
        // Verify deleted from database
        assertFalse(eventRepository.findById(created.getId()).isPresent());
    }
    
    @Test
    void testDeleteEventNotFoundReturnsFalse() {
        boolean deleted = eventService.deleteEvent("nonexistent");
        
        assertFalse(deleted);
    }
    
    // ============= Helper Methods =============
    
    private EventDTO createTestEvent(String title, LocalDateTime startTime, String pageId) {
        EventDTO dto = new EventDTO();
        dto.setId("event-" + System.nanoTime());
        dto.setTitle(title);
        dto.setStartTime(startTime);
        dto.setPageId(pageId);
        return eventService.createEvent(dto);
    }
    
    private EventDTO createTestEventWithPlace(String title, LocalDateTime startTime, String placeId) {
        EventDTO dto = new EventDTO();
        dto.setId("event-" + System.nanoTime());
        dto.setTitle(title);
        dto.setStartTime(startTime);
        dto.setPageId(testPage.getId()); // Events must have a page
        
        PlaceDTO placeDTO = new PlaceDTO();
        placeDTO.setId(placeId);
        dto.setPlace(placeDTO);
        
        return eventService.createEvent(dto);
    }
}
