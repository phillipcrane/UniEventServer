package dk.unievent.web.service;

import dk.unievent.web.dto.EventDTO;
import dk.unievent.web.mapper.EventMapper;
import dk.unievent.web.model.EventEntity;
import dk.unievent.web.model.PageEntity;
import dk.unievent.web.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTests {
    
    @Mock
    private EventRepository eventRepository;
    
    @Mock
    private EventMapper eventMapper;
    
    @InjectMocks
    private EventService eventService;
    
    private EventEntity testEventEntity;
    private EventDTO testEventDTO;
    private PageEntity testPage;
    
    @BeforeEach
    void setUp() {
        testPage = PageEntity.builder()
                .id("page-1")
                .name("Test Page")
                .build();
        
        testEventEntity = EventEntity.builder()
                .id("event-1")
                .title("Test Event")
                .description("Test Description")
                .page(testPage)
                .startTime(LocalDateTime.now().plusHours(1))
                .build();
        
        testEventDTO = new EventDTO();
        testEventDTO.setId("event-1");
        testEventDTO.setTitle("Test Event");
        testEventDTO.setDescription("Test Description");
        testEventDTO.setPageId("page-1");
        testEventDTO.setStartTime(LocalDateTime.now().plusHours(1));
    }
    
    @Test
    void testGetAllEvents() {
        EventEntity event2 = EventEntity.builder()
                .id("event-2")
                .title("Event 2")
                .page(testPage)
                .build();
        
        List<EventEntity> eventEntities = List.of(testEventEntity, event2);
        when(eventRepository.findAllByOrderByStartTimeAsc()).thenReturn(eventEntities);
        
        EventDTO dto2 = new EventDTO();
        dto2.setId("event-2");
        dto2.setTitle("Event 2");
        
        when(eventMapper.toDTO(testEventEntity)).thenReturn(testEventDTO);
        when(eventMapper.toDTO(event2)).thenReturn(dto2);
        
        List<EventDTO> result = eventService.getAllEvents();
        
        assertEquals(2, result.size());
        assertEquals("event-1", result.get(0).getId());
        assertEquals("event-2", result.get(1).getId());
        verify(eventRepository, times(1)).findAllByOrderByStartTimeAsc();
        verify(eventMapper, times(2)).toDTO(any());
    }
    
    @Test
    void testGetAllEventsEmpty() {
        when(eventRepository.findAllByOrderByStartTimeAsc()).thenReturn(List.of());
        
        List<EventDTO> result = eventService.getAllEvents();
        
        assertTrue(result.isEmpty());
        verify(eventRepository, times(1)).findAllByOrderByStartTimeAsc();
    }
    
    @Test
    void testGetFutureEvents() {
        List<EventEntity> futureEvents = List.of(testEventEntity);
        when(eventRepository.findByStartTimeGreaterThanEqualOrderByStartTimeAsc(any(LocalDateTime.class)))
            .thenReturn(futureEvents);
        when(eventMapper.toDTO(testEventEntity)).thenReturn(testEventDTO);
        
        List<EventDTO> result = eventService.getFutureEvents();
        
        assertEquals(1, result.size());
        assertEquals("event-1", result.get(0).getId());
        verify(eventRepository, times(1))
            .findByStartTimeGreaterThanEqualOrderByStartTimeAsc(any(LocalDateTime.class));
    }
    
    @Test
    void testGetEventById() {
        when(eventRepository.findById("event-1")).thenReturn(Optional.of(testEventEntity));
        when(eventMapper.toDTO(testEventEntity)).thenReturn(testEventDTO);
        
        EventDTO result = eventService.getEventById("event-1");
        
        assertNotNull(result);
        assertEquals("event-1", result.getId());
        assertEquals("Test Event", result.getTitle());
        verify(eventRepository, times(1)).findById("event-1");
        verify(eventMapper, times(1)).toDTO(testEventEntity);
    }
    
    @Test
    void testGetEventByIdNotFound() {
        when(eventRepository.findById("non-existent")).thenReturn(Optional.empty());
        
        EventDTO result = eventService.getEventById("non-existent");
        
        assertNull(result);
        verify(eventRepository, times(1)).findById("non-existent");
    }
    
    @Test
    void testGetEventsByPageId() {
        List<EventEntity> pageEvents = List.of(testEventEntity);
        when(eventRepository.findByPageIdOrderByStartTimeAsc("page-1"))
            .thenReturn(pageEvents);
        when(eventMapper.toDTO(testEventEntity)).thenReturn(testEventDTO);
        
        List<EventDTO> result = eventService.getEventsByPageId("page-1");
        
        assertEquals(1, result.size());
        assertEquals("event-1", result.get(0).getId());
        verify(eventRepository, times(1)).findByPageIdOrderByStartTimeAsc("page-1");
    }
    
    @Test
    void testGetEventsByPageIdEmpty() {
        when(eventRepository.findByPageIdOrderByStartTimeAsc("page-2"))
            .thenReturn(List.of());
        
        List<EventDTO> result = eventService.getEventsByPageId("page-2");
        
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testGetFutureEventsByPageId() {
        List<EventEntity> futurePageEvents = List.of(testEventEntity);
        when(eventRepository.findByPageIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(
            eq("page-1"), any(LocalDateTime.class)))
            .thenReturn(futurePageEvents);
        when(eventMapper.toDTO(testEventEntity)).thenReturn(testEventDTO);
        
        List<EventDTO> result = eventService.getFutureEventsByPageId("page-1");
        
        assertEquals(1, result.size());
        verify(eventRepository, times(1))
            .findByPageIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(
                eq("page-1"), any(LocalDateTime.class));
    }
    
    @Test
    void testGetEventsByPlaceId() {
        List<EventEntity> placeEvents = List.of(testEventEntity);
        when(eventRepository.findByPlaceIdOrderByStartTimeAsc("place-1"))
            .thenReturn(placeEvents);
        when(eventMapper.toDTO(testEventEntity)).thenReturn(testEventDTO);
        
        List<EventDTO> result = eventService.getEventsByPlaceId("place-1");
        
        assertEquals(1, result.size());
        verify(eventRepository, times(1)).findByPlaceIdOrderByStartTimeAsc("place-1");
    }
    
    @Test
    void testCreateEvent() {
        when(eventMapper.toEntity(testEventDTO)).thenReturn(testEventEntity);
        when(eventRepository.save(testEventEntity)).thenReturn(testEventEntity);
        when(eventMapper.toDTO(testEventEntity)).thenReturn(testEventDTO);
        
        EventDTO result = eventService.createEvent(testEventDTO);
        
        assertNotNull(result);
        assertEquals("event-1", result.getId());
        verify(eventMapper, times(1)).toEntity(testEventDTO);
        verify(eventRepository, times(1)).save(testEventEntity);
        verify(eventMapper, times(1)).toDTO(testEventEntity);
    }
    
    @Test
    void testUpdateEvent() {
        when(eventRepository.findById("event-1")).thenReturn(Optional.of(testEventEntity));
        when(eventRepository.save(any(EventEntity.class))).thenReturn(testEventEntity);
        when(eventMapper.toDTO(testEventEntity)).thenReturn(testEventDTO);
        
        EventDTO updateDTO = new EventDTO();
        updateDTO.setTitle("Updated Title");
        updateDTO.setDescription("Updated Description");
        
        EventDTO result = eventService.updateEvent("event-1", updateDTO);
        
        assertNotNull(result);
        assertEquals("event-1", result.getId());
        verify(eventRepository, times(1)).findById("event-1");
        verify(eventRepository, times(1)).save(any(EventEntity.class));
    }
    
    @Test
    void testUpdateEventNotFound() {
        when(eventRepository.findById("non-existent")).thenReturn(Optional.empty());
        
        EventDTO updateDTO = new EventDTO();
        updateDTO.setTitle("Updated Title");
        
        EventDTO result = eventService.updateEvent("non-existent", updateDTO);
        
        assertNull(result);
        verify(eventRepository, times(1)).findById("non-existent");
        verify(eventRepository, never()).save(any());
    }
    
    @Test
    void testDeleteEvent() {
        when(eventRepository.existsById("event-1")).thenReturn(true);
        
        boolean result = eventService.deleteEvent("event-1");
        
        assertTrue(result);
        verify(eventRepository, times(1)).existsById("event-1");
        verify(eventRepository, times(1)).deleteById("event-1");
    }
    
    @Test
    void testDeleteEventNotFound() {
        when(eventRepository.existsById("non-existent")).thenReturn(false);
        
        boolean result = eventService.deleteEvent("non-existent");
        
        assertFalse(result);
        verify(eventRepository, times(1)).existsById("non-existent");
        verify(eventRepository, never()).deleteById("non-existent");
    }
    
    @Test
    void testUpdateEventTime() {
        when(eventRepository.findById("event-1")).thenReturn(Optional.of(testEventEntity));
        when(eventRepository.save(any(EventEntity.class))).thenReturn(testEventEntity);
        when(eventMapper.toDTO(testEventEntity)).thenReturn(testEventDTO);
        
        LocalDateTime newStartTime = LocalDateTime.now().plusHours(3);
        LocalDateTime newEndTime = LocalDateTime.now().plusHours(5);
        
        EventDTO updateDTO = new EventDTO();
        updateDTO.setStartTime(newStartTime);
        updateDTO.setEndTime(newEndTime);
        
        EventDTO result = eventService.updateEvent("event-1", updateDTO);
        
        assertNotNull(result);
        verify(eventRepository, times(1)).save(any(EventEntity.class));
    }
}
