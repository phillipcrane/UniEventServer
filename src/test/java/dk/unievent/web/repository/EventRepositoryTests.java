package dk.unievent.web.repository;

import dk.unievent.web.model.EventEntity;
import dk.unievent.web.model.PageEntity;
import dk.unievent.web.model.PlaceEntity;
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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventRepositoryTests {

    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private PageRepository pageRepository;
    
    @Autowired
    private PlaceRepository placeRepository;
    
    private PageEntity testPage;
    private PlaceEntity testPlace;
    private EventEntity testEvent;
    
    @BeforeEach
    void setUp() {
        // Create and persist test page
        testPage = new PageEntity();
        testPage.setId("page-1");
        testPage.setName("Test Page");
        testPage.setTokenStatus("valid");
        pageRepository.save(testPage);

        
        // Create and persist test place
        testPlace = new PlaceEntity();
        testPlace.setId("place-1");
        testPlace.setName("Test Place");
        testPlace.setCity("Test City");
        testPlace.setCountry("Test Country");
        placeRepository.save(testPlace);

        
        // Create and persist test event
        testEvent = new EventEntity();
        testEvent.setId("event-1");
        testEvent.setTitle("Test Event");
        testEvent.setDescription("Test Description");
        testEvent.setPage(testPage);
        testEvent.setPlace(testPlace);
        testEvent.setStartTime(LocalDateTime.now().plusHours(1));
        testEvent.setEndTime(LocalDateTime.now().plusHours(2));
        eventRepository.save(testEvent);

    }
    
    @Test
    void testSaveEvent() {
        EventEntity event = new EventEntity();
        event.setId("event-2");
        event.setTitle("New Event");
        event.setDescription("New Description");
        event.setPage(testPage);
        event.setStartTime(LocalDateTime.now());
        
        EventEntity savedEvent = eventRepository.save(event);
        
        assertNotNull(savedEvent);
        assertEquals("event-2", savedEvent.getId());
        assertEquals("New Event", savedEvent.getTitle());
        assertNotNull(savedEvent.getCreatedAt());
    }
    
    @Test
    void testFindEventById() {
        Optional<EventEntity> foundEvent = eventRepository.findById("event-1");
        
        assertTrue(foundEvent.isPresent());
        assertEquals("Test Event", foundEvent.get().getTitle());
        assertEquals("event-1", foundEvent.get().getId());
    }
    
    @Test
    void testFindEventByIdNotFound() {
        Optional<EventEntity> foundEvent = eventRepository.findById("non-existent");
        
        assertTrue(foundEvent.isEmpty());
    }
    
    @Test
    void testFindAllEvents() {
        EventEntity event2 = new EventEntity();
        event2.setId("event-2");
        event2.setTitle("Second Event");
        event2.setPage(testPage);
        event2.setStartTime(LocalDateTime.now().plusHours(3));
        eventRepository.save(event2);

        
        List<EventEntity> events = eventRepository.findAll();
        
        assertEquals(2, events.size());
    }
    
    @Test
    void testFindAllByOrderByStartTimeAsc() {
        LocalDateTime now = LocalDateTime.now();
        
        EventEntity event2 = new EventEntity();
        event2.setId("event-2");
        event2.setTitle("Later Event");
        event2.setPage(testPage);
        event2.setStartTime(now.plusHours(5));
        eventRepository.save(event2);
        
        EventEntity event3 = new EventEntity();
        event3.setId("event-3");
        event3.setTitle("Even Later Event");
        event3.setPage(testPage);
        event3.setStartTime(now.plusHours(10));
        eventRepository.save(event3);

        
        List<EventEntity> events = eventRepository.findAllByOrderByStartTimeAsc();
        
        assertEquals(3, events.size());
        assertTrue(events.get(0).getStartTime().isBefore(events.get(1).getStartTime()));
        assertTrue(events.get(1).getStartTime().isBefore(events.get(2).getStartTime()));
    }
    
    @Test
    void testFindByStartTimeGreaterThanEqualOrderByStartTimeAsc() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureTime = now.plusHours(5);
        
        EventEntity pastEvent = new EventEntity();
        pastEvent.setId("event-past");
        pastEvent.setTitle("Past Event");
        pastEvent.setPage(testPage);
        pastEvent.setStartTime(now.minusHours(1));
        eventRepository.save(pastEvent);
        
        EventEntity futureEvent = new EventEntity();
        futureEvent.setId("event-future");
        futureEvent.setTitle("Future Event");
        futureEvent.setPage(testPage);
        futureEvent.setStartTime(futureTime.plusHours(1));
        eventRepository.save(futureEvent);

        
        List<EventEntity> futureEvents = eventRepository.findByStartTimeGreaterThanEqualOrderByStartTimeAsc(futureTime);
        
        assertEquals(1, futureEvents.size());
        assertTrue(futureEvents.stream().allMatch(e -> !e.getStartTime().isBefore(futureTime)));
    }
    
    @Test
    void testFindByPageIdOrderByStartTimeAsc() {
        PageEntity page2 = new PageEntity();
        page2.setId("page-2");
        page2.setName("Page 2");
        pageRepository.save(page2);
        
        EventEntity event2 = new EventEntity();
        event2.setId("event-2");
        event2.setTitle("Page 2 Event");
        event2.setPage(page2);
        event2.setStartTime(LocalDateTime.now().plusHours(5));
        eventRepository.save(event2);

        
        List<EventEntity> pageEvents = eventRepository.findByPageIdOrderByStartTimeAsc("page-1");
        
        assertEquals(1, pageEvents.size());
        assertEquals("event-1", pageEvents.get(0).getId());
    }
    
    @Test
    void testFindByPageIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc() {
        LocalDateTime futureTime = LocalDateTime.now().plusHours(5);
        
        EventEntity futureEvent = new EventEntity();
        futureEvent.setId("event-future");
        futureEvent.setTitle("Future Event");
        futureEvent.setPage(testPage);
        futureEvent.setStartTime(futureTime.plusHours(1));
        eventRepository.save(futureEvent);

        
        List<EventEntity> pageEvents = eventRepository.findByPageIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(
            "page-1", futureTime);
        
        assertEquals(1, pageEvents.size());
        assertEquals("event-future", pageEvents.get(0).getId());
    }
    
    @Test
    void testFindByPlaceIdOrderByStartTimeAsc() {
        PlaceEntity place2 = new PlaceEntity();
        place2.setId("place-2");
        place2.setName("Place 2");
        placeRepository.save(place2);
        
        EventEntity event2 = new EventEntity();
        event2.setId("event-2");
        event2.setTitle("Other Place Event");
        event2.setPage(testPage);
        event2.setPlace(place2);
        event2.setStartTime(LocalDateTime.now().plusHours(5));
        eventRepository.save(event2);

        
        List<EventEntity> placeEvents = eventRepository.findByPlaceIdOrderByStartTimeAsc("place-1");
        
        assertEquals(1, placeEvents.size());
        assertEquals("event-1", placeEvents.get(0).getId());
    }
    
    @Test
    void testUpdateEvent() {
        EventEntity event = eventRepository.findById("event-1").orElseThrow();
        LocalDateTime originalUpdatedAt = event.getUpdatedAt();
        
        // Update the event
        event.setTitle("Updated Title");
        event.setDescription("Updated Description");
        
        try {
            Thread.sleep(10); // Small delay to ensure timestamp difference
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        eventRepository.save(event);

        
        EventEntity updatedEvent = eventRepository.findById("event-1").orElseThrow();
        
        assertEquals("Updated Title", updatedEvent.getTitle());
        assertEquals("Updated Description", updatedEvent.getDescription());
        assertTrue(updatedEvent.getUpdatedAt().isAfter(originalUpdatedAt) || 
                   updatedEvent.getUpdatedAt().isEqual(originalUpdatedAt));
    }
    
    @Test
    void testDeleteEvent() {
        eventRepository.deleteById("event-1");

        
        Optional<EventEntity> deletedEvent = eventRepository.findById("event-1");
        
        assertTrue(deletedEvent.isEmpty());
    }
    
    @Test
    void testEventWithoutPlace() {
        EventEntity event = new EventEntity();
        event.setId("event-no-place");
        event.setTitle("Event Without Place");
        event.setPage(testPage);
        event.setStartTime(LocalDateTime.now());
        // place is null
        
        EventEntity savedEvent = eventRepository.save(event);

        
        EventEntity retrieved = eventRepository.findById("event-no-place").orElseThrow();
        assertNull(retrieved.getPlace());
        assertNotNull(retrieved.getPage());
    }
    
    @Test
    void testEventStartTimeAndEndTime() {
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(2);
        
        EventEntity event = new EventEntity();
        event.setId("event-times");
        event.setTitle("Event With Times");
        event.setPage(testPage);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        
        EventEntity savedEvent = eventRepository.save(event);

        
        EventEntity retrieved = eventRepository.findById("event-times").orElseThrow();
        assertEquals(startTime, retrieved.getStartTime());
        assertEquals(endTime, retrieved.getEndTime());
    }
}

