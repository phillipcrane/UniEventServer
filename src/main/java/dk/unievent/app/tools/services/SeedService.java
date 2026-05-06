package dk.unievent.app.tools.services;

import dk.unievent.app.application.service.MediaService;
import dk.unievent.app.db.model.EventEntity;
import dk.unievent.app.db.model.MediaEntity;
import dk.unievent.app.db.model.PageEntity;
import dk.unievent.app.db.model.PlaceEntity;
import dk.unievent.app.db.repository.EventRepository;
import dk.unievent.app.db.repository.MediaRepository;
import dk.unievent.app.db.repository.PageRepository;
import dk.unievent.app.db.repository.PlaceRepository;
import dk.unievent.app.tools.models.SeedResponse;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service for seeding and clearing test data locally.
 * All seeded records are marked with "SEED_" prefix for easy identification and removal.
 */
@Slf4j
@Service
public class SeedService {

    private final EventRepository eventRepository;
    private final PageRepository pageRepository;
    private final PlaceRepository placeRepository;
    private final MediaRepository mediaRepository;
    private final MediaService mediaService;
    private final EntityManager entityManager;

    private static final String SEED_PREFIX = "SEED_";
    private static final String PLACEHOLDER_IMAGE_URL = "https://placehold.co/600x400.jpg";

    public SeedService(EventRepository eventRepository, PageRepository pageRepository,
                       PlaceRepository placeRepository, MediaRepository mediaRepository,
                       MediaService mediaService, EntityManager entityManager) {
        this.eventRepository = eventRepository;
        this.pageRepository = pageRepository;
        this.placeRepository = placeRepository;
        this.mediaRepository = mediaRepository;
        this.mediaService = mediaService;
        this.entityManager = entityManager;
    }

    /**
     * Seeds minimal test data to the database.
     * Creates 10 media files (all referencing the same SeaweedFS image from media/1),
     * 2 pages, 10 events, and 2 places with realistic test data.
     */
    @Transactional
    public SeedResponse seedData() {
        log.info("Starting data seed operation...");
        try {
            // Clear any existing seed data first so seeding is idempotent
            clearExistingSeedRecords();

            // Try to upload a placeholder image to SeaweedFS; if unavailable, proceed without media
            String sharedFileId = null;
            try {
                sharedFileId = mediaService.downloadAndStoreImage(PLACEHOLDER_IMAGE_URL, "SEED_placeholder.jpg");
                log.info("Placeholder image uploaded to SeaweedFS: {}", sharedFileId);
            } catch (Exception e) {
                log.warn("SeaweedFS unavailable - seeding without cover images: {}", e.getMessage());
            }

            // Create 10 unique media records all pointing to the same image (or null if upload failed)
            MediaEntity media1  = sharedFileId != null ? createAndSaveMedia("SEED_react_workshop.jpg",    "image/jpeg", sharedFileId) : null;
            MediaEntity media2  = sharedFileId != null ? createAndSaveMedia("SEED_spring_boot.jpg",       "image/jpeg", sharedFileId) : null;
            MediaEntity media3  = sharedFileId != null ? createAndSaveMedia("SEED_docker_k8s.jpg",        "image/jpeg", sharedFileId) : null;
            MediaEntity media4  = sharedFileId != null ? createAndSaveMedia("SEED_ai_ml.jpg",             "image/jpeg", sharedFileId) : null;
            MediaEntity media5  = sharedFileId != null ? createAndSaveMedia("SEED_graphql.jpg",           "image/jpeg", sharedFileId) : null;
            MediaEntity media6  = sharedFileId != null ? createAndSaveMedia("SEED_jazz_night.jpg",        "image/jpeg", sharedFileId) : null;
            MediaEntity media7  = sharedFileId != null ? createAndSaveMedia("SEED_art_exhibition.jpg",    "image/jpeg", sharedFileId) : null;
            MediaEntity media8  = sharedFileId != null ? createAndSaveMedia("SEED_film_festival.jpg",     "image/jpeg", sharedFileId) : null;
            MediaEntity media9  = sharedFileId != null ? createAndSaveMedia("SEED_classical_concert.jpg", "image/jpeg", sharedFileId) : null;
            MediaEntity media10 = sharedFileId != null ? createAndSaveMedia("SEED_book_club.jpg",         "image/jpeg", sharedFileId) : null;

            // Create places (UUIDs auto-generated; use SEED_ name prefix for identification)
            PlaceEntity copenhagenPlace = createAndSavePlace("SEED_Copenhagen", "Nørrebro", "1200", "Denmark", 55.6761, 12.5683);
            PlaceEntity aarhusPlace = createAndSavePlace("SEED_Aarhus", "Centrum", "8000", "Denmark", 56.1629, 10.2039);

            // Create pages
            PageEntity techEventsPage = createAndSavePage("SEED_TECH_EVENTS", "Tech Events");
            PageEntity cultureEventsPage = createAndSavePage("SEED_CULTURE_EVENTS", "Culture Events");

            // Create events for Tech Events page
            LocalDateTime now = LocalDateTime.now();
            createAndSaveEvent("SEED_EVENT_001", "React Workshop", "Deep dive into modern React patterns",
                now.plus(7, ChronoUnit.DAYS).withHour(10).withMinute(0),
                now.plus(7, ChronoUnit.DAYS).withHour(12).withMinute(0),
                techEventsPage, copenhagenPlace, media1);

            createAndSaveEvent("SEED_EVENT_002", "Spring Boot Masterclass", "Advanced Spring Boot techniques",
                now.plus(14, ChronoUnit.DAYS).withHour(9).withMinute(30),
                now.plus(14, ChronoUnit.DAYS).withHour(17).withMinute(30),
                techEventsPage, copenhagenPlace, media2);

            createAndSaveEvent("SEED_EVENT_003", "Docker & Kubernetes 101", "Container orchestration fundamentals",
                now.plus(21, ChronoUnit.DAYS).withHour(14).withMinute(0),
                now.plus(21, ChronoUnit.DAYS).withHour(16).withMinute(0),
                techEventsPage, aarhusPlace, media3);

            createAndSaveEvent("SEED_EVENT_004", "AI & Machine Learning", "Introduction to ML in production",
                now.plus(30, ChronoUnit.DAYS).withHour(10).withMinute(0),
                now.plus(30, ChronoUnit.DAYS).withHour(16).withMinute(0),
                techEventsPage, copenhagenPlace, media4);

            createAndSaveEvent("SEED_EVENT_005", "GraphQL Best Practices", "Building scalable GraphQL systems",
                now.plus(45, ChronoUnit.DAYS).withHour(13).withMinute(0),
                now.plus(45, ChronoUnit.DAYS).withHour(15).withMinute(0),
                techEventsPage, null, media5);

            // Create events for Culture Events page
            createAndSaveEvent("SEED_EVENT_006", "Jazz Night", "Live jazz performance with local musicians",
                now.plus(5, ChronoUnit.DAYS).withHour(20).withMinute(0),
                now.plus(5, ChronoUnit.DAYS).withHour(23).withMinute(0),
                cultureEventsPage, copenhagenPlace, media6);

            createAndSaveEvent("SEED_EVENT_007", "Art Exhibition Opening", "Contemporary art exhibition preview",
                now.plus(10, ChronoUnit.DAYS).withHour(18).withMinute(0),
                now.plus(10, ChronoUnit.DAYS).withHour(21).withMinute(0),
                cultureEventsPage, aarhusPlace, media7);

            createAndSaveEvent("SEED_EVENT_008", "Film Festival", "Screening of indie films from around the world",
                now.plus(20, ChronoUnit.DAYS).withHour(19).withMinute(0),
                now.plus(20, ChronoUnit.DAYS).withHour(22).withMinute(0),
                cultureEventsPage, copenhagenPlace, media8);

            createAndSaveEvent("SEED_EVENT_009", "Classical Music Concert", "Danish Philharmonic Orchestra performance",
                now.plus(25, ChronoUnit.DAYS).withHour(19).withMinute(30),
                now.plus(25, ChronoUnit.DAYS).withHour(21).withMinute(30),
                cultureEventsPage, copenhagenPlace, media9);

            createAndSaveEvent("SEED_EVENT_010", "Book Club Meeting", "Discussion of contemporary Nordic literature",
                now.plus(35, ChronoUnit.DAYS).withHour(17).withMinute(0),
                now.plus(35, ChronoUnit.DAYS).withHour(19).withMinute(0),
                cultureEventsPage, null, media10);

            String msg = sharedFileId != null ? "Seed data created successfully" : "Seed data created (no cover images - SeaweedFS unavailable)";
            SeedResponse result = new SeedResponse(true, msg, 2, 10, 2);
            log.info("Data seed completed: {} pages, {} events, {} places, media={}", result.getPageCount(), result.getEventCount(), result.getPlaceCount(), sharedFileId != null ? "10 records" : "skipped");
            return result;
        } catch (Exception e) {
            log.error("Error during data seed operation", e);
            return new SeedResponse(false, "Error during seed operation: " + e.getMessage(), 0, 0, 0);
        }
    }

    /**
     * Clears all seeded test data from the database.
     * Deletes only records marked with SEED_ prefix.
     */
    @Transactional
    public SeedResponse clearSeedData() {
        log.info("Starting data cleanup operation...");
        try {
            // Delete events first (they reference pages and places and media)
            List<EventEntity> seededEvents = eventRepository.findAll().stream()
                .filter(e -> e.getId().startsWith(SEED_PREFIX))
                .toList();
            eventRepository.deleteAll(seededEvents);
            long deletedEvents = seededEvents.size();

            // Delete pages (events must be deleted first due to foreign key)
            List<PageEntity> seededPages = pageRepository.findAll().stream()
                .filter(p -> p.getId().startsWith(SEED_PREFIX))
                .toList();
            pageRepository.deleteAll(seededPages);
            long deletedPages = seededPages.size();

            // Delete places (events must be deleted first due to foreign key)
            List<PlaceEntity> seededPlaces = placeRepository.findAll().stream()
                .filter(p -> p.getName() != null && p.getName().startsWith(SEED_PREFIX))
                .toList();
            placeRepository.deleteAll(seededPlaces);
            long deletedPlaces = seededPlaces.size();

            // Delete seeded media (must be after events since they reference it)
            List<MediaEntity> seededMedia = mediaRepository.findAll().stream()
                .filter(m -> m.getFilename() != null && m.getFilename().startsWith(SEED_PREFIX))
                .toList();
            // Delete unique fileIds from SeaweedFS (all seed records share one file)
            seededMedia.stream()
                .map(MediaEntity::getFileId)
                .filter(fid -> fid != null && !fid.isBlank())
                .distinct()
                .forEach(fid -> {
                    try { mediaService.delete(fid); } catch (Exception e) { log.warn("Could not delete seed file from SeaweedFS: {}", fid, e); }
                });
            mediaRepository.deleteAll(seededMedia);
            long deletedMedia = seededMedia.size();

            SeedResponse result = new SeedResponse(true, "Seed data cleared successfully",
                deletedPages, deletedEvents, deletedPlaces);
            log.info("Data cleanup completed: deleted {} pages, {} events, {} places, {} media",
                deletedPages, deletedEvents, deletedPlaces, deletedMedia);
            return result;
        } catch (Exception e) {
            log.error("Error during data cleanup operation", e);
            return new SeedResponse(false, "Error during cleanup operation: " + e.getMessage(), 0, 0, 0);
        }
    }

    private void clearExistingSeedRecords() {
        eventRepository.deleteAll(eventRepository.findAll().stream()
            .filter(e -> e.getId().startsWith(SEED_PREFIX)).toList());
        pageRepository.deleteAll(pageRepository.findAll().stream()
            .filter(p -> p.getId().startsWith(SEED_PREFIX)).toList());
        placeRepository.deleteAll(placeRepository.findAll().stream()
            .filter(p -> p.getName() != null && p.getName().startsWith(SEED_PREFIX)).toList());
        List<MediaEntity> staleMedia = mediaRepository.findAll().stream()
            .filter(m -> m.getFilename() != null && m.getFilename().startsWith(SEED_PREFIX)).toList();
        staleMedia.stream().map(MediaEntity::getFileId).distinct().forEach(fid -> {
            if (fid == null || fid.isBlank()) {
                return;
            }
            try { mediaService.delete(fid); } catch (Exception e) { log.warn("Could not delete seed file from SeaweedFS: {}", fid, e); }
        });
        mediaRepository.deleteAll(staleMedia);
        entityManager.flush();
        entityManager.clear();
    }

    private MediaEntity createAndSaveMedia(String filename, String contentType, String fileId) {
        MediaEntity media = MediaEntity.builder()
            .filename(filename)
            .contentType(contentType)
            .fileId(fileId)
            .uploadedAt(Instant.now())
            .build();
        return mediaRepository.save(media);
    }

    private PlaceEntity createAndSavePlace(String name, String street, String zip, String country, Double latitude, Double longitude) {
        PlaceEntity place = PlaceEntity.builder()
            .id(java.util.UUID.randomUUID().toString())
            .name(name)
            .street(street)
            .zip(zip)
            .country(country)
            .latitude(latitude)
            .longitude(longitude)
            .build();
        return placeRepository.save(place);
    }

    private PageEntity createAndSavePage(String id, String name) {
        PageEntity page = PageEntity.builder()
            .id(id)
            .name(name)
            .tokenStatus("valid")
            .build();
        entityManager.persist(page);
        return page;
    }

    private EventEntity createAndSaveEvent(String id, String title, String description,
                                          LocalDateTime startTime, LocalDateTime endTime,
                                          PageEntity page, PlaceEntity place, MediaEntity coverImage) {
        EventEntity event = EventEntity.builder()
            .id(id)
            .title(title)
            .description(description)
            .startTime(startTime)
            .endTime(endTime)
            .page(page)
            .place(place)
            .coverImage(coverImage)
            .build();
        entityManager.persist(event);
        return event;
    }
}
