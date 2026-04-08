package dk.unievent.app.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import dk.unievent.app.application.dto.EventDTO;
import dk.unievent.app.application.mapper.EventMapper;
import dk.unievent.app.application.mapper.PlaceMapper;
import dk.unievent.app.db.model.EventEntity;
import dk.unievent.app.db.model.MediaEntity;
import dk.unievent.app.db.model.PageEntity;
import dk.unievent.app.db.repository.EventRepository;
import dk.unievent.app.db.repository.MediaRepository;
import dk.unievent.app.db.repository.PageRepository;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final PlaceMapper placeMapper;
    private final MediaRepository mediaRepository;
    private final PageRepository pageRepository;
    private final MediaService mediaService;

    public EventService(
            EventRepository eventRepository,
            EventMapper eventMapper,
            PlaceMapper placeMapper,
            MediaRepository mediaRepository,
            PageRepository pageRepository,
            MediaService mediaService) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.placeMapper = placeMapper;
        this.mediaRepository = mediaRepository;
        this.pageRepository = pageRepository;
        this.mediaService = mediaService;
    }

    public Page<EventDTO> getAllEvents(Pageable pageable) {
        log.debug("Fetching all events with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<EventDTO> result = eventRepository.findAllByOrderByStartTimeAsc(pageable)
                .map(eventMapper::toDTO);
        log.debug("Found {} events", result.getTotalElements());
        return result;
    }

    public Page<EventDTO> getFutureEvents(Pageable pageable) {
        log.debug("Fetching future events with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<EventDTO> result = eventRepository.findByStartTimeGreaterThanEqualOrderByStartTimeAsc(LocalDateTime.now(), pageable)
                .map(eventMapper::toDTO);
        log.debug("Found {} future events", result.getTotalElements());
        return result;
    }

    public EventDTO getEventById(String id) {
        log.debug("Fetching event with id: {}", id);
        Optional<EventEntity> entity = eventRepository.findById(id);
        if (entity.isEmpty()) {
            log.debug("Event not found with id: {}", id);
            return null;
        }
        log.debug("Event found: {}", id);
        return entity.map(eventMapper::toDTO).orElse(null);
    }

    public Page<EventDTO> getEventsByPageId(String pageId, Pageable pageable) {
        log.debug("Fetching events for pageId: {}, page: {}, size: {}", pageId, pageable.getPageNumber(), pageable.getPageSize());
        Page<EventDTO> result = eventRepository.findByPageIdOrderByStartTimeAsc(pageId, pageable)
                .map(eventMapper::toDTO);
        log.debug("Found {} events for page: {}", result.getTotalElements(), pageId);
        return result;
    }

    public Page<EventDTO> getFutureEventsByPageId(String pageId, Pageable pageable) {
        log.debug("Fetching future events for pageId: {}, page: {}, size: {}", pageId, pageable.getPageNumber(), pageable.getPageSize());
        Page<EventDTO> result = eventRepository.findByPageIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(pageId, LocalDateTime.now(), pageable)
                .map(eventMapper::toDTO);
        log.debug("Found {} future events for page: {}", result.getTotalElements(), pageId);
        return result;
    }

    public Page<EventDTO> getEventsByPlaceId(String placeId, Pageable pageable) {
        log.debug("Fetching events for placeId: {}", placeId);
        Page<EventDTO> result = eventRepository.findByPlaceIdOrderByStartTimeAsc(placeId, pageable)
                .map(eventMapper::toDTO);
        log.debug("Found {} events for place: {}", result.getTotalElements(), placeId);
        return result;
    }

    public EventDTO createEvent(EventDTO eventDTO) {
        log.info("Creating new event: {}", eventDTO.getTitle());
        EventEntity entity = eventMapper.toEntity(eventDTO);

        if (eventDTO.getPageId() != null) {
            entity.setPage(getPageOrThrow(eventDTO.getPageId()));
        }

        if (eventDTO.getCoverImageId() != null) {
            MediaEntity coverImage = mediaRepository.findById(eventDTO.getCoverImageId()).orElse(null);
            entity.setCoverImage(coverImage);
        }

        EventEntity saved = eventRepository.save(entity);
        log.info("Event created successfully with id: {}", saved.getId());
        return eventMapper.toDTO(saved);
    }

    public EventDTO updateEvent(String id, EventDTO eventDTO) {
        log.info("Updating event with id: {}", id);
        Optional<EventEntity> existing = eventRepository.findById(id);
        if (existing.isEmpty()) {
            log.warn("Event not found for update with id: {}", id);
            return null;
        }

        EventEntity entity = existing.get();
        entity.setTitle(eventDTO.getTitle());
        entity.setDescription(eventDTO.getDescription());
        entity.setStartTime(eventDTO.getStartTime());
        entity.setEndTime(eventDTO.getEndTime());
        entity.setEventUrl(eventDTO.getEventUrl());

        if (eventDTO.getPlace() != null) {
            entity.setPlace(placeMapper.toEntity(eventDTO.getPlace()));
        }

        if (eventDTO.getPageId() != null) {
            entity.setPage(getPageOrThrow(eventDTO.getPageId()));
        }

        if (eventDTO.getCoverImageId() != null) {
            MediaEntity coverImage = mediaRepository.findById(eventDTO.getCoverImageId()).orElse(null);
            entity.setCoverImage(coverImage);
        }

        EventEntity updated = eventRepository.save(entity);
        log.info("Event updated successfully: {}", id);
        return eventMapper.toDTO(updated);
    }

    public EventDTO uploadCoverImage(String id, MultipartFile file) throws IOException {
        log.info("Uploading cover image for event: {}", id);
        Optional<EventEntity> existing = eventRepository.findById(id);
        if (existing.isEmpty()) {
            log.warn("Event not found for cover image upload with id: {}", id);
            return null;
        }

        String storedFilename = mediaService.store(file);
        MediaEntity mediaEntity = MediaEntity.builder()
                .filename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileId(storedFilename)
                .uploadedAt(Instant.now())
                .build();
        MediaEntity savedMedia = mediaRepository.save(mediaEntity);

        EventEntity entity = existing.get();
        entity.setCoverImage(savedMedia);
        EventEntity updated = eventRepository.save(entity);
        log.info("Cover image uploaded successfully for event: {}", id);
        return eventMapper.toDTO(updated);
    }

    public boolean deleteEvent(String id) {
        log.info("Deleting event with id: {}", id);
        if (eventRepository.existsById(id)) {
            eventRepository.deleteById(id);
            log.info("Event deleted successfully: {}", id);
            return true;
        }
        log.warn("Event not found for deletion with id: {}", id);
        return false;
    }

    private PageEntity getPageOrThrow(String pageId) {
        return pageRepository.findById(pageId)
                .orElseThrow(() -> new NoSuchElementException("Page not found: " + pageId));
    }
}
