package dk.unievent.app.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import dk.unievent.app.api.dto.FbEventResponse;
import dk.unievent.app.application.dto.EventDTO;
import dk.unievent.app.application.mapper.EventMapper;
import dk.unievent.app.application.mapper.FacebookEventMapper;
import dk.unievent.app.application.mapper.PlaceMapper;
import dk.unievent.app.db.model.EventEntity;
import dk.unievent.app.db.model.MediaEntity;
import dk.unievent.app.db.model.PageEntity;
import dk.unievent.app.db.repository.EventRepository;
import dk.unievent.app.db.repository.MediaRepository;
import dk.unievent.app.db.repository.PageRepository;
import dk.unievent.app.infrastructure.exception.FacebookApiException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

// CRUD and Facebook ingestion for events. Ingestion pulls from the Graph API, downloads cover
// images into SeaweedFS, and upserts the EventEntity so re-ingesting the same event is safe.
@Slf4j
@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final PlaceMapper placeMapper;
    private final MediaRepository mediaRepository;
    private final PageRepository pageRepository;
    private final MediaService mediaService;
    private final FacebookEventMapper facebookEventMapper;
    private final FacebookGraphApiService facebookGraphApiService;
    private final VaultService vaultService;

    public EventService(
            EventRepository eventRepository,
            EventMapper eventMapper,
            PlaceMapper placeMapper,
            MediaRepository mediaRepository,
            PageRepository pageRepository,
            MediaService mediaService,
            FacebookEventMapper facebookEventMapper,
            FacebookGraphApiService facebookGraphApiService,
            VaultService vaultService) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.placeMapper = placeMapper;
        this.mediaRepository = mediaRepository;
        this.pageRepository = pageRepository;
        this.mediaService = mediaService;
        this.facebookEventMapper = facebookEventMapper;
        this.facebookGraphApiService = facebookGraphApiService;
        this.vaultService = vaultService;
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

    public Optional<EventDTO> getEventById(String id) {
        log.debug("Fetching event with id: {}", id);
        Optional<EventEntity> entity = eventRepository.findById(id);
        if (entity.isEmpty()) {
            log.debug("Event not found with id: {}", id);
            return Optional.empty();
        }
        log.debug("Event found: {}", id);
        return entity.map(eventMapper::toDTO);
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

    @Transactional
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

    @Transactional
    public Optional<EventDTO> updateEvent(String id, EventDTO eventDTO) {
        log.info("Updating event with id: {}", id);
        Optional<EventEntity> existing = eventRepository.findById(id);
        if (existing.isEmpty()) {
            log.warn("Event not found for update with id: {}", id);
            return Optional.empty();
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
        return Optional.of(eventMapper.toDTO(updated));
    }

    @Transactional
    public Optional<EventDTO> uploadCoverImage(String id, MultipartFile file) throws IOException {
        log.info("Uploading cover image for event: {}", id);
        Optional<EventEntity> existing = eventRepository.findById(id);
        if (existing.isEmpty()) {
            log.warn("Event not found for cover image upload with id: {}", id);
            return Optional.empty();
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
        MediaEntity oldMedia = entity.getCoverImage();
        entity.setCoverImage(savedMedia);
        EventEntity updated = eventRepository.save(entity);

        if (oldMedia != null) {
            try {
                mediaService.delete(oldMedia.getFileId());
            } catch (IOException e) {
                log.warn("Failed to delete old cover image from SeaweedFS: {}", oldMedia.getFileId(), e);
            }
        }

        log.info("Cover image uploaded successfully for event: {}", id);
        return Optional.of(eventMapper.toDTO(updated));
    }

    @Transactional
    public boolean deleteEvent(String id) {
        log.info("Deleting event with id: {}", id);
        Optional<EventEntity> event = eventRepository.findById(id);
        if (event.isEmpty()) {
            log.warn("Event not found for deletion with id: {}", id);
            return false;
        }
        MediaEntity coverImage = event.get().getCoverImage();
        eventRepository.deleteById(id);
        if (coverImage != null) {
            try {
                mediaService.delete(coverImage.getFileId());
            } catch (IOException e) {
                log.warn("Failed to delete cover image from SeaweedFS: {}", coverImage.getFileId(), e);
            }
        }
        log.info("Event deleted successfully: {}", id);
        return true;
    }

    private PageEntity getPageOrThrow(String pageId) {
        return pageRepository.findById(pageId)
                .orElseThrow(() -> new NoSuchElementException("Page not found: " + pageId));
    }

    // pulls events from the Facebook Graph API for a given page and upserts them locally.
    // individual event failures are swallowed so one bad event doesn't stop the rest.
    public List<EventEntity> ingestFacebookEvents(String pageId) {
        log.info("Starting Facebook event ingestion for page: {}", pageId);

        try {
            // 1. get the page token from Vault. TOKEN_NOT_FOUND means the page hasn't been connected yet
            Optional<String> pageTokenOpt = vaultService.getPageToken(pageId);
            if (pageTokenOpt.isEmpty()) {
                log.warn("No token found in Vault for page: {}", pageId);
                throw new FacebookApiException(
                    "No token found for page: " + pageId,
                    0,
                    "TOKEN_NOT_FOUND"
                );
            }

            String pageToken = pageTokenOpt.get();

            // 2. pull the event list from the Graph API
            log.debug("Fetching events from Facebook for page: {}", pageId);
            List<FbEventResponse> fbEvents = facebookGraphApiService.getPageEvents(pageId, pageToken);
            log.info("Retrieved {} events from Facebook for page: {}", fbEvents.size(), pageId);

            // 3. upsert each event locally, skipping any that fail individually
            List<EventEntity> processedEvents = fbEvents.stream()
                .map(fbEvent -> {
                    try {
                        return createOrUpdateFacebookEvent(pageId, fbEvent);
                    } catch (Exception e) {
                        log.error("Error creating/updating event from Facebook: {}", fbEvent.getId(), e);
                        return null;
                    }
                })
                .filter(event -> event != null)
                .collect(Collectors.toList());

            log.info("Facebook event ingestion completed. Processed: {}/{} events",
                processedEvents.size(), fbEvents.size());
            return processedEvents;

        } catch (FacebookApiException e) {
            if ("TOKEN_NOT_FOUND".equals(e.getErrorType())) {
                log.warn("Facebook token missing during event ingestion for page: {} - {} ({})",
                        pageId, e.getErrorType(), e.getStatusCode());
            } else {
                log.error("Facebook API error during event ingestion: {} - {}",
                    e.getStatusCode(), e.getErrorType(), e);
            }
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during Facebook event ingestion for page: {}", pageId, e);
            throw new FacebookApiException(
                "Event ingestion failed: " + e.getMessage(),
                0,
                "INGESTION_ERROR"
            );
        }
    }

    @Transactional
    public EventEntity createOrUpdateFacebookEvent(String pageId, FbEventResponse fbEvent) {
        log.debug("Processing Facebook event: {} ({})", fbEvent.getName(), fbEvent.getId());

        try {
            // 1. map the Graph API response to an EventEntity and wire up the page reference
            Optional<EventEntity> existing = eventRepository.findById(fbEvent.getId());
            EventEntity eventEntity = facebookEventMapper.mapToEventEntity(pageId, fbEvent);
            eventEntity.setPage(getPageOrThrow(pageId));

            // 2. download the cover image if the URL changed (reuse the existing MediaEntity if it's the same URL)
            MediaEntity oldCoverImage = existing.map(EventEntity::getCoverImage).orElse(null);
            if (fbEvent.getCover() != null && fbEvent.getCover().getSource() != null) {
                String imageUrl = fbEvent.getCover().getSource();
                if (oldCoverImage != null && imageUrl.equals(oldCoverImage.getSourceUrl())) {
                    eventEntity.setCoverImage(oldCoverImage);
                    log.debug("Cover image URL unchanged for event {}, reusing existing", fbEvent.getId());
                } else {
                    try {
                        log.debug("Downloading cover image for event: {}", fbEvent.getId());
                        String filename = String.format("fb_event_%s.jpg", fbEvent.getId());
                        MediaEntity coverImage = downloadAndStoreCoverImage(imageUrl, filename);
                        if (coverImage != null) {
                            eventEntity.setCoverImage(coverImage);
                            log.debug("Cover image stored for event: {}", fbEvent.getId());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to download cover image for event: {}", fbEvent.getId(), e);
                    }
                }
            }

            // 3. persist, then delete the old cover image from SeaweedFS after the new one is committed
            EventEntity saved = eventRepository.save(eventEntity);
            if (oldCoverImage != null && (eventEntity.getCoverImage() == null
                    || !oldCoverImage.getId().equals(eventEntity.getCoverImage().getId()))) {
                try {
                    mediaService.delete(oldCoverImage.getFileId());
                    mediaRepository.delete(oldCoverImage);
                    log.debug("Deleted orphaned cover image {} for event: {}", oldCoverImage.getFileId(), fbEvent.getId());
                } catch (Exception e) {
                    log.warn("Failed to delete orphaned cover image {} for event: {}", oldCoverImage.getFileId(), fbEvent.getId(), e);
                }
            }
            String action = existing.isPresent() ? "updated" : "created";
            log.info("Facebook event {} successfully: {} ({})", action, saved.getTitle(), saved.getId());

            return saved;

        } catch (Exception e) {
            log.error("Error creating/updating event from Facebook: {}", fbEvent.getId(), e);
            throw new RuntimeException(
                String.format("Failed to process Facebook event %s: %s", fbEvent.getId(), e.getMessage()),
                e
            );
        }
    }

    // downloads a cover image from a URL and stores it in SeaweedFS. returns null on failure so callers can skip gracefully.
    private MediaEntity downloadAndStoreCoverImage(String imageUrl, String filename) {
        try {
            log.debug("Downloading cover image from URL: {}", imageUrl);
            String storedFileId = mediaService.downloadAndStoreImage(imageUrl, filename);
            
            MediaEntity mediaEntity = MediaEntity.builder()
                .filename(filename)
                .contentType("image/jpeg")
                .fileId(storedFileId)
                .sourceUrl(imageUrl)
                .uploadedAt(Instant.now())
                .build();

            MediaEntity savedMedia = mediaRepository.save(mediaEntity);
            log.debug("Cover image stored successfully with file ID: {}", storedFileId);
            return savedMedia;

        } catch (Exception e) {
            log.warn("Failed to download and store cover image from URL: {}", imageUrl, e);
            return null;
        }
    }
}
