package dk.unievent.web.service;

import dk.unievent.web.entity.Event;
import dk.unievent.web.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    private Event event;

    @BeforeEach
    void setUp() {
        event = new Event();
        event.setId(1L);
        event.setTitle("Tech Talk");
        event.setDescription("A tech talk about Java");
        event.setStartTime(LocalDateTime.now().plusDays(1));
        event.setLocation("Auditorium");
    }

    @Test
    void getAllEvents_shouldReturnAllEvents() {
        when(eventRepository.findAll()).thenReturn(Arrays.asList(event));

        List<Event> result = eventService.getAllEvents();

        assertThat(result).hasSize(1);
        verify(eventRepository).findAll();
    }

    @Test
    void getEventById_shouldReturnEvent_whenExists() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        Optional<Event> result = eventService.getEventById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Tech Talk");
    }

    @Test
    void getEventById_shouldReturnEmpty_whenNotExists() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Event> result = eventService.getEventById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void createEvent_shouldSaveAndReturnEvent() {
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        Event result = eventService.createEvent(event);

        assertThat(result.getTitle()).isEqualTo("Tech Talk");
        verify(eventRepository).save(event);
    }

    @Test
    void updateEvent_shouldUpdateAndReturnEvent_whenExists() {
        Event updatedDetails = new Event();
        updatedDetails.setTitle("Updated Tech Talk");
        updatedDetails.setDescription("Updated description");
        updatedDetails.setStartTime(LocalDateTime.now().plusDays(2));
        updatedDetails.setLocation("Room B");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        Event result = eventService.updateEvent(1L, updatedDetails);

        assertThat(result).isNotNull();
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void updateEvent_shouldThrowException_whenNotExists() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.updateEvent(99L, event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Event not found with id: 99");
    }

    @Test
    void deleteEvent_shouldCallDeleteById() {
        doNothing().when(eventRepository).deleteById(1L);

        eventService.deleteEvent(1L);

        verify(eventRepository).deleteById(1L);
    }

    @Test
    void searchEventsByTitle_shouldReturnMatchingEvents() {
        when(eventRepository.findByTitleContainingIgnoreCase("tech")).thenReturn(Arrays.asList(event));

        List<Event> result = eventService.searchEventsByTitle("tech");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Tech Talk");
    }

    @Test
    void getUpcomingEvents_shouldReturnFutureEvents() {
        LocalDateTime now = LocalDateTime.now();
        when(eventRepository.findByStartTimeAfter(now)).thenReturn(Arrays.asList(event));

        List<Event> result = eventService.getUpcomingEvents(now);

        assertThat(result).hasSize(1);
    }
}
