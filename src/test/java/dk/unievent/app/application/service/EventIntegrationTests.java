package dk.unievent.app.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import dk.unievent.app.api.dto.FbEventResponse;
import dk.unievent.app.application.dto.EventDTO;
import dk.unievent.app.application.dto.PlaceDTO;
import dk.unievent.app.db.model.EventEntity;
import dk.unievent.app.db.model.PageEntity;
import dk.unievent.app.db.model.PlaceEntity;
import dk.unievent.app.db.repository.EventRepository;
import dk.unievent.app.db.repository.PageRepository;
import dk.unievent.app.db.repository.PlaceRepository;

import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

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

    @MockitoBean
    private FacebookGraphApiService facebookGraphApiService;

    @MockitoBean
    private VaultService vaultService;
    
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
        
        Page<EventDTO> allEvents = eventService.getAllEvents(PageRequest.of(0, 20));
        
        assertEquals(3, allEvents.getContent().size());
        // Verify ordering by startTime
        assertEquals(event3.getId(), allEvents.getContent().get(0).getId()); // 15th
        assertEquals(event1.getId(), allEvents.getContent().get(1).getId()); // 20th
        assertEquals(event2.getId(), allEvents.getContent().get(2).getId()); // 25th
    }
    
    @Test
    void testGetFutureEventsThroughService() {
        LocalDateTime now = LocalDateTime.now();
        
        // Create past and future events
        createTestEvent("Past Event", now.minusDays(1), testPage.getId());
        createTestEvent("Future Event 1", now.plusDays(3), testPage.getId());
        createTestEvent("Future Event 2", now.plusDays(7), testPage.getId());
        
        Page<EventDTO> futureEvents = eventService.getFutureEvents(PageRequest.of(0, 20));
        
        assertEquals(2, futureEvents.getContent().size());
        assertTrue(futureEvents.getContent().stream().allMatch(e -> e.getStartTime().isAfter(now)));
    }
    
    @Test
    void testGetEventByIdThroughService() {
        EventDTO created = createTestEvent("Find Me", LocalDateTime.now().plusDays(1), testPage.getId());
        
        Optional<EventDTO> found = eventService.getEventById(created.getId());

        assertTrue(found.isPresent());
        assertEquals("Find Me", found.get().getTitle());
        assertEquals(created.getId(), found.get().getId());
    }
    
    @Test
    void testGetEventByIdNotFoundReturnsNull() {
        Optional<EventDTO> notFound = eventService.getEventById("nonexistent");

        assertTrue(notFound.isEmpty());
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
        
        Page<EventDTO> page1Events = eventService.getEventsByPageId(testPage.getId(), PageRequest.of(0, 20));
        Page<EventDTO> page2Events = eventService.getEventsByPageId(page2.getId(), PageRequest.of(0, 20));
        
        assertEquals(2, page1Events.getContent().size());
        assertEquals(1, page2Events.getContent().size());
        assertTrue(page1Events.getContent().stream().allMatch(e -> e.getPageId().equals(testPage.getId())));
        assertTrue(page2Events.getContent().stream().allMatch(e -> e.getPageId().equals(page2.getId())));
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
        
        Page<EventDTO> place1Events = eventService.getEventsByPlaceId(testPlace.getId(), PageRequest.of(0, 20));
        Page<EventDTO> place2Events = eventService.getEventsByPlaceId(place2.getId(), PageRequest.of(0, 20));
        
        assertEquals(1, place1Events.getContent().size());
        assertEquals(1, place2Events.getContent().size());
    }
    
    // ============= Update Tests =============
    
    @Test
    void testUpdateEventThroughService() {
        EventDTO created = createTestEvent("Original Title", LocalDateTime.now().plusDays(1), testPage.getId());
        
        EventDTO updateDTO = new EventDTO();
        updateDTO.setTitle("Updated Title");
        updateDTO.setDescription("Updated Desc");
        
        Optional<EventDTO> updated = eventService.updateEvent(created.getId(), updateDTO);

        assertTrue(updated.isPresent());
        assertEquals("Updated Title", updated.get().getTitle());
        assertEquals("Updated Desc", updated.get().getDescription());
        assertEquals(created.getId(), updated.get().getId());
    }
    
    @Test
    void testUpdateEventNotFoundReturnsNull() {
        EventDTO updateDTO = new EventDTO();
        updateDTO.setTitle("New Title");
        
        Optional<EventDTO> result = eventService.updateEvent("nonexistent", updateDTO);

        assertTrue(result.isEmpty());
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

    @Test
    void ingestFacebookEventsShouldPersistImportedEvents() {
        FbEventResponse fbEvent = new FbEventResponse();
        fbEvent.setId("fb-event-1");
        fbEvent.setName("Imported Facebook Event");
        fbEvent.setDescription("Imported through Graph API");
        fbEvent.setStartTime(OffsetDateTime.parse("2030-05-01T18:00:00+02:00"));
        fbEvent.setEndTime(OffsetDateTime.parse("2030-05-01T20:00:00+02:00"));
        fbEvent.setTimezone("Europe/Copenhagen");

        when(vaultService.getPageToken(testPage.getId())).thenReturn(Optional.of("page-token"));
        when(facebookGraphApiService.getPageEvents(testPage.getId(), "page-token")).thenReturn(java.util.List.of(fbEvent));

        java.util.List<EventEntity> imported = eventService.ingestFacebookEvents(testPage.getId());

        assertEquals(1, imported.size());
        Optional<EventEntity> persisted = eventRepository.findById("fb-event-1");
        assertTrue(persisted.isPresent());
        assertEquals("Imported Facebook Event", persisted.get().getTitle());
        assertEquals(testPage.getId(), persisted.get().getPage().getId());
        assertEquals("https://facebook.com/events/fb-event-1", persisted.get().getEventUrl());
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
