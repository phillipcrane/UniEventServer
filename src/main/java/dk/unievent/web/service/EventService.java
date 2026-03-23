package dk.unievent.web.service;

import dk.unievent.web.dto.EventDTO;
import dk.unievent.web.mapper.EventMapper;
import dk.unievent.web.model.EventEntity;
import dk.unievent.web.model.MediaEntity;
import dk.unievent.web.repository.MediaRepository;
import dk.unievent.web.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EventService {
    
    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private EventMapper eventMapper;
    
    @Autowired
    private MediaRepository mediaRepository;
    
    /**
     * Get all events ordered by start time
     */
    public List<EventDTO> getAllEvents() {
        return eventRepository.findAllByOrderByStartTimeAsc()
            .stream()
            .map(eventMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all future events (startTime >= now) ordered by start time
     */
    public List<EventDTO> getFutureEvents() {
        return eventRepository.findByStartTimeGreaterThanEqualOrderByStartTimeAsc(LocalDateTime.now())
            .stream()
            .map(eventMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Get event by ID
     */
    public EventDTO getEventById(String id) {
        Optional<EventEntity> entity = eventRepository.findById(id);
        return entity.map(eventMapper::toDTO).orElse(null);
    }
    
    /**
     * Get all events from a specific page
     */
    public List<EventDTO> getEventsByPageId(String pageId) {
        return eventRepository.findByPageIdOrderByStartTimeAsc(pageId)
            .stream()
            .map(eventMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Get future events from a specific page
     */
    public List<EventDTO> getFutureEventsByPageId(String pageId) {
        return eventRepository.findByPageIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(pageId, LocalDateTime.now())
            .stream()
            .map(eventMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all events at a specific place
     */
    public List<EventDTO> getEventsByPlaceId(String placeId) {
        return eventRepository.findByPlaceIdOrderByStartTimeAsc(placeId)
            .stream()
            .map(eventMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Create a new event
     */
    public EventDTO createEvent(EventDTO eventDTO) {
        EventEntity entity = eventMapper.toEntity(eventDTO);
        EventEntity saved = eventRepository.save(entity);
        return eventMapper.toDTO(saved);
    }
    
    /**
     * Update an existing event
     */
    public EventDTO updateEvent(String id, EventDTO eventDTO) {
        Optional<EventEntity> existing = eventRepository.findById(id);
        if (existing.isEmpty()) {
            return null;
        }
        
        EventEntity entity = existing.get();
        entity.setTitle(eventDTO.getTitle());
        entity.setDescription(eventDTO.getDescription());
        entity.setStartTime(eventDTO.getStartTime());
        entity.setEndTime(eventDTO.getEndTime());
        entity.setEventURL(eventDTO.getEventURL());
        
        // Update cover image if provided
        if (eventDTO.getCoverImageId() != null) {
            MediaEntity coverImage = mediaRepository.findById(eventDTO.getCoverImageId()).orElse(null);
            entity.setCoverImage(coverImage);
        }
        
        EventEntity updated = eventRepository.save(entity);
        return eventMapper.toDTO(updated);
    }
    
    /**
     * Delete an event
     */
    public boolean deleteEvent(String id) {
        if (eventRepository.existsById(id)) {
            eventRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
