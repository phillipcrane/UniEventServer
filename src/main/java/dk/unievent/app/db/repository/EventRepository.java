package dk.unievent.app.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.unievent.app.db.model.EventEntity;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, String> {
    
    /**
     * Find all events ordered by start time (ascending)
     */
    List<EventEntity> findAllByOrderByStartTimeAsc();
    
    /**
     * Find all future events (startTime >= now) ordered by start time
     */
    List<EventEntity> findByStartTimeGreaterThanEqualOrderByStartTimeAsc(LocalDateTime startTime);
    
    /**
     * Find events by page ID
     */
    List<EventEntity> findByPageIdOrderByStartTimeAsc(String pageId);
    
    /**
     * Find events by page ID and future start time
     */
    List<EventEntity> findByPageIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(String pageId, LocalDateTime startTime);
    
    /**
     * Find events by place ID
     */
    List<EventEntity> findByPlaceIdOrderByStartTimeAsc(String placeId);
}
