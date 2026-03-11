package dk.unievent.web.repository;

import dk.unievent.web.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByTitleContainingIgnoreCase(String title);

    List<Event> findByStartTimeAfter(LocalDateTime startTime);

    List<Event> findByLocation(String location);
}
