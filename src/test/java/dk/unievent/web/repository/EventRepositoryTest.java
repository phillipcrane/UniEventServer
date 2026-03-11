package dk.unievent.web.repository;

import dk.unievent.web.entity.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    private Event testEvent;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        testEvent = new Event();
        testEvent.setTitle("Spring Boot Workshop");
        testEvent.setDescription("Learn Spring Boot 4.x");
        testEvent.setStartTime(LocalDateTime.now().plusDays(1));
        testEvent.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        testEvent.setLocation("Room A101");
    }

    @Test
    void saveEvent_shouldPersistEvent() {
        Event saved = eventRepository.save(testEvent);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Spring Boot Workshop");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findById_shouldReturnEvent_whenEventExists() {
        Event saved = eventRepository.save(testEvent);

        Optional<Event> found = eventRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Spring Boot Workshop");
    }

    @Test
    void findById_shouldReturnEmpty_whenEventDoesNotExist() {
        Optional<Event> found = eventRepository.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void findByTitleContainingIgnoreCase_shouldReturnMatchingEvents() {
        eventRepository.save(testEvent);
        Event another = new Event();
        another.setTitle("Java Conference");
        another.setStartTime(LocalDateTime.now().plusDays(2));
        eventRepository.save(another);

        List<Event> results = eventRepository.findByTitleContainingIgnoreCase("spring");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Spring Boot Workshop");
    }

    @Test
    void findByTitleContainingIgnoreCase_shouldBeCaseInsensitive() {
        eventRepository.save(testEvent);

        List<Event> results = eventRepository.findByTitleContainingIgnoreCase("SPRING BOOT");

        assertThat(results).hasSize(1);
    }

    @Test
    void findByStartTimeAfter_shouldReturnFutureEvents() {
        eventRepository.save(testEvent);

        List<Event> results = eventRepository.findByStartTimeAfter(LocalDateTime.now());

        assertThat(results).hasSize(1);
    }

    @Test
    void findByStartTimeAfter_shouldNotReturnPastEvents() {
        Event pastEvent = new Event();
        pastEvent.setTitle("Past Event");
        pastEvent.setStartTime(LocalDateTime.now().minusDays(1));
        eventRepository.save(pastEvent);

        List<Event> results = eventRepository.findByStartTimeAfter(LocalDateTime.now());

        assertThat(results).isEmpty();
    }

    @Test
    void findByLocation_shouldReturnEventsAtLocation() {
        eventRepository.save(testEvent);

        List<Event> results = eventRepository.findByLocation("Room A101");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getLocation()).isEqualTo("Room A101");
    }

    @Test
    void deleteById_shouldRemoveEvent() {
        Event saved = eventRepository.save(testEvent);

        eventRepository.deleteById(saved.getId());

        assertThat(eventRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllEvents() {
        eventRepository.save(testEvent);
        Event another = new Event();
        another.setTitle("Another Event");
        another.setStartTime(LocalDateTime.now().plusDays(3));
        eventRepository.save(another);

        List<Event> all = eventRepository.findAll();

        assertThat(all).hasSize(2);
    }
}
