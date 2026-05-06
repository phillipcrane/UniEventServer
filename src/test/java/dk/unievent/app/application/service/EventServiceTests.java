package dk.unievent.app.application.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import dk.unievent.app.application.dto.EventDTO;
import dk.unievent.app.application.mapper.EventMapper;
import dk.unievent.app.db.model.EventEntity;
import dk.unievent.app.db.model.MediaEntity;
import dk.unievent.app.db.model.PageEntity;
import dk.unievent.app.db.repository.EventRepository;
import dk.unievent.app.db.repository.MediaRepository;
import dk.unievent.app.db.repository.PageRepository;

import java.io.IOException;
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

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private PageRepository pageRepository;

    @Mock
    private MediaService mediaService;
    
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
        Page<EventEntity> eventPage = new PageImpl<>(eventEntities, PageRequest.of(0, 20), 2);
        when(eventRepository.findAllByOrderByStartTimeAsc(any(Pageable.class))).thenReturn(eventPage);
        
        EventDTO dto2 = new EventDTO();
        dto2.setId("event-2");
        dto2.setTitle("Event 2");
        
        when(eventMapper.toDTO(testEventEntity)).thenReturn(testEventDTO);
        when(eventMapper.toDTO(event2)).thenReturn(dto2);
        
        Page<EventDTO> result = eventService.getAllEvents(PageRequest.of(0, 20));
        
        assertEquals(2, result.getContent().size());
        assertEquals("event-1", result.getContent().get(0).getId());
        assertEquals("event-2", result.getContent().get(1).getId());
        verify(eventRepository, times(1)).findAllByOrderByStartTimeAsc(any(Pageable.class));
        verify(eventMapper, times(2)).toDTO(any());
    }
    
    @Test
    void testGetAllEventsEmpty() {
        Page<EventEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(eventRepository.findAllByOrderByStartTimeAsc(any(Pageable.class))).thenReturn(emptyPage);
        
        Page<EventDTO> result = eventService.getAllEvents(PageRequest.of(0, 20));
        
        assertTrue(result.getContent().isEmpty());
        verify(eventRepository, times(1)).findAllByOrderByStartTimeAsc(any(Pageable.class));
    }
    
    @Test
    void testGetFutureEvents() {
        List<EventEntity> futureEvents = List.of(testEventEntity);
        Page<EventEntity> futurePage = new PageImpl<>(futureEvents, PageRequest.of(0, 20), 1);
        when(eventRepository.findByStartTimeGreaterThanEqualOrderByStartTimeAsc(any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(futurePage);
        when(eventMapper.toDTO(testEventEntity)).thenReturn(testEventDTO);
        
        Page<EventDTO> result = eventService.getFutureEvents(PageRequest.of(0, 20));
        
        assertEquals(1, result.getContent().size());
        assertEquals("event-1", result.getContent().get(0).getId());
        verify(eventRepository, times(1))
            .findByStartTimeGreaterThanEqualOrderByStartTimeAsc(any(LocalDateTime.class), any(Pageable.class));
    }
    
    @Test
    void testGetEventById() {
        when(eventRepository.findById("event-1")).thenReturn(Optional.of(testEventEntity));
        when(eventMapper.toDTO(testEventEntity)).thenReturn(testEventDTO);
        
        Optional<EventDTO> result = eventService.getEventById("event-1");
        
        assertTrue(result.isPresent());
        assertEquals("event-1", result.get().getId());
        assertEquals("Test Event", result.get().getTitle());
        verify(eventRepository, times(1)).findById("event-1");
        verify(eventMapper, times(1)).toDTO(testEventEntity);
    }
    
    @Test
    void testGetEventByIdNotFound() {
        when(eventRepository.findById("non-existent")).thenReturn(Optional.empty());
        
        Optional<EventDTO> result = eventService.getEventById("non-existent");
        
        assertTrue(result.isEmpty());
        verify(eventRepository, times(1)).findById("non-existent");
    }
    
    @Test
    void testGetEventsByPageId() {
        List<EventEntity> pageEvents = List.of(testEventEntity);
        Page<EventEntity> pageEventPage = new PageImpl<>(pageEvents, PageRequest.of(0, 20), 1);
        when(eventRepository.findByPageIdOrderByStartTimeAsc(eq("page-1"), any(Pageable.class)))
            .thenReturn(pageEventPage);
        when(eventMapper.toDTO(testEventEntity)).thenReturn(testEventDTO);
        
        Page<EventDTO> result = eventService.getEventsByPageId("page-1", PageRequest.of(0, 20));
        
        assertEquals(1, result.getContent().size());
        assertEquals("event-1", result.getContent().get(0).getId());
        verify(eventRepository, times(1)).findByPageIdOrderByStartTimeAsc(eq("page-1"), any(Pageable.class));
    }
    
    @Test
    void testGetEventsByPageIdEmpty() {
        Page<EventEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(eventRepository.findByPageIdOrderByStartTimeAsc(eq("page-2"), any(Pageable.class)))
            .thenReturn(emptyPage);
        
        Page<EventDTO> result = eventService.getEventsByPageId("page-2", PageRequest.of(0, 20));
        
        assertTrue(result.getContent().isEmpty());
    }
    
    @Test
    void testGetFutureEventsByPageId() {
        List<EventEntity> futurePageEvents = List.of(testEventEntity);
        Page<EventEntity> futurePageEventPage = new PageImpl<>(futurePageEvents, PageRequest.of(0, 20), 1);
        when(eventRepository.findByPageIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(
            eq("page-1"), any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(futurePageEventPage);
        when(eventMapper.toDTO(testEventEntity)).thenReturn(testEventDTO);
        
        Page<EventDTO> result = eventService.getFutureEventsByPageId("page-1", PageRequest.of(0, 20));
        
        assertEquals(1, result.getContent().size());
        verify(eventRepository, times(1))
            .findByPageIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(
                eq("page-1"), any(LocalDateTime.class), any(Pageable.class));
    }
    
    @Test
    void testGetEventsByPlaceId() {
        List<EventEntity> placeEvents = List.of(testEventEntity);
        Page<EventEntity> placeEventPage = new PageImpl<>(placeEvents, PageRequest.of(0, 20), 1);
        when(eventRepository.findByPlaceIdOrderByStartTimeAsc(eq("place-1"), any(Pageable.class)))
            .thenReturn(placeEventPage);
        when(eventMapper.toDTO(testEventEntity)).thenReturn(testEventDTO);
        
        Page<EventDTO> result = eventService.getEventsByPlaceId("place-1", PageRequest.of(0, 20));
        
        assertEquals(1, result.getContent().size());
        verify(eventRepository, times(1)).findByPlaceIdOrderByStartTimeAsc(eq("place-1"), any(Pageable.class));
        verify(eventMapper, times(1)).toDTO(testEventEntity);
    }
    
    @Test
    void testCreateEvent() {
        when(eventMapper.toEntity(testEventDTO)).thenReturn(testEventEntity);
        when(pageRepository.findById("page-1")).thenReturn(Optional.of(testPage));
        when(eventRepository.save(testEventEntity)).thenReturn(testEventEntity);
        when(eventMapper.toDTO(testEventEntity)).thenReturn(testEventDTO);
        
        EventDTO result = eventService.createEvent(testEventDTO);
        
        assertNotNull(result);
        assertEquals("event-1", result.getId());
        verify(eventMapper, times(1)).toEntity(testEventDTO);
        verify(pageRepository, times(1)).findById("page-1");
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
        
        Optional<EventDTO> result = eventService.updateEvent("event-1", updateDTO);
        
        assertTrue(result.isPresent());
        assertEquals("event-1", result.get().getId());
        verify(eventRepository, times(1)).findById("event-1");
        verify(eventRepository, times(1)).save(any(EventEntity.class));
    }
    
    @Test
    void testUpdateEventNotFound() {
        when(eventRepository.findById("non-existent")).thenReturn(Optional.empty());
        
        EventDTO updateDTO = new EventDTO();
        updateDTO.setTitle("Updated Title");
        
        Optional<EventDTO> result = eventService.updateEvent("non-existent", updateDTO);
        
        assertTrue(result.isEmpty());
        verify(eventRepository, times(1)).findById("non-existent");
        verify(eventRepository, never()).save(any());
    }

    @Test
    void uploadCoverImageShouldReplaceCoverImageAndDeleteOldFile() throws IOException {
        MediaEntity oldCoverImage = MediaEntity.builder().id(1L).fileId("1,old").filename("old.jpg").build();
        testEventEntity.setCoverImage(oldCoverImage);
        MockMultipartFile file = new MockMultipartFile("file", "new.png", "image/png",
                new byte[] {(byte) 0x89, 'P', 'N', 'G'});
        when(eventRepository.findById("event-1")).thenReturn(Optional.of(testEventEntity));
        when(mediaService.store(file)).thenReturn("1,new");
        when(mediaRepository.save(any(MediaEntity.class))).thenAnswer(invocation -> {
            MediaEntity saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        when(eventRepository.save(testEventEntity)).thenReturn(testEventEntity);
        when(eventMapper.toDTO(testEventEntity)).thenReturn(testEventDTO);

        Optional<EventDTO> result = eventService.uploadCoverImage("event-1", file);

        assertTrue(result.isPresent());
        assertEquals("1,new", testEventEntity.getCoverImage().getFileId());
        verify(mediaService).delete("1,old");
    }

    @Test
    void deleteEventShouldDeleteCoverImageAndContinueWhenStorageDeleteFails() throws IOException {
        MediaEntity coverImage = MediaEntity.builder().id(1L).fileId("1,missing").filename("cover.jpg").build();
        testEventEntity.setCoverImage(coverImage);
        when(eventRepository.findById("event-1")).thenReturn(Optional.of(testEventEntity));
        doThrow(new IOException("already deleted")).when(mediaService).delete("1,missing");

        boolean result = eventService.deleteEvent("event-1");

        assertTrue(result);
        verify(eventRepository).deleteById("event-1");
        verify(mediaService).delete("1,missing");
    }
    
    @Test
    void testDeleteEvent() {
        when(eventRepository.findById("event-1")).thenReturn(Optional.of(testEventEntity));

        boolean result = eventService.deleteEvent("event-1");

        assertTrue(result);
        verify(eventRepository, times(1)).findById("event-1");
        verify(eventRepository, times(1)).deleteById("event-1");
    }

    @Test
    void testDeleteEventNotFound() {
        when(eventRepository.findById("non-existent")).thenReturn(Optional.empty());

        boolean result = eventService.deleteEvent("non-existent");

        assertFalse(result);
        verify(eventRepository, times(1)).findById("non-existent");
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
        
        Optional<EventDTO> result = eventService.updateEvent("event-1", updateDTO);
        
        assertTrue(result.isPresent());
        verify(eventRepository, times(1)).save(any(EventEntity.class));
    }
}
