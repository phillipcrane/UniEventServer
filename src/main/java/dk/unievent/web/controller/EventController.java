package dk.unievent.web.controller;

import dk.unievent.web.dto.EventDTO;
import dk.unievent.web.service.EventService;
import dk.unievent.web.model.MediaEntity;
import dk.unievent.web.service.MediaService;
import dk.unievent.web.repository.MediaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * REST Controller for Event endpoints
 * All endpoints are prefixed with /api/events
 * 
 * Example requests:
 * GET  /api/events              - Get all events
 * GET  /api/events/future       - Get only upcoming events
 * GET  /api/events/{id}         - Get event by ID
 * GET  /api/events/page/{pageId} - Get events from specific organizer
 * POST /api/events              - Create new event
 * PUT  /api/events/{id}         - Update event
 * DELETE /api/events/{id}       - Delete event
 */
@RestController
@RequestMapping("/api/events")
@Tag(name = "Events", description = "Manage and retrieve events")
public class EventController {
    
    @Autowired
    private EventService eventService;
    
    @Autowired
    private MediaService mediaService;
    
    @Autowired
    private MediaRepository mediaRepository;
    
    /**
     * GET /api/events
     * Returns ALL events ordered by start time (ascending)
     * 
     * Frontend calls this when loading the main page
     * Example response: [{ id: "evt-1", title: "Pub Night", startTime: "2026-03-15T20:00:00" }, ...]
     */
    @GetMapping
    @Operation(summary = "Get all events", description = "Retrieve all events ordered by start time")
    @ApiResponse(responseCode = "200", description = "List of all events")
    public ResponseEntity<List<EventDTO>> getAllEvents() {
        List<EventDTO> events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }
    
    /**
     * GET /api/events/future
     * Returns ONLY future events (startTime >= now)
     * This is what the frontend probably wants - no past events
     * 
     * Useful for: Main event feed
     * Example: GET /api/events/future
     * Returns: List of EventDTO ordered by startTime
     */
    @GetMapping("/future")
    @Operation(summary = "Get future events", description = "Retrieve only upcoming events (startTime >= now)")
    @ApiResponse(responseCode = "200", description = "List of future events")
    public ResponseEntity<List<EventDTO>> getFutureEvents() {
        List<EventDTO> events = eventService.getFutureEvents();
        return ResponseEntity.ok(events);
    }
    
    /**
     * GET /api/events/{id}
     * Get a SINGLE event by ID
     * 
     * Parameter: id (path variable) = event ID from database
     * Example: GET /api/events/event-123
     * Returns: Single EventDTO or 404 if not found
     * 
     * Useful for: Event detail page
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID", description = "Retrieve a single event by its ID")
    @ApiResponse(responseCode = "200", description = "Event found")
    @ApiResponse(responseCode = "404", description = "Event not found")
    public ResponseEntity<EventDTO> getEventById(@PathVariable @Parameter(description = "Event ID") String id) {
        EventDTO event = eventService.getEventById(id);
        
        // If event not found, return 404
        if (event == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(event);
    }
    
    /**
     * GET /api/events/page/{pageId}
     * Get ALL events from a specific ORGANIZER (page)
     * 
     * Parameter: pageId = Facebook page ID
     * Example: GET /api/events/page/123456789
     * Returns: List of EventDTO from that page
     * 
     * Useful for: Filter events by organizer
     */
    @GetMapping("/page/{pageId}")
    @Operation(summary = "Get events by page", description = "Retrieve all events from a specific organizer (page)")
    @ApiResponse(responseCode = "200", description = "List of events from the page")
    public ResponseEntity<List<EventDTO>> getEventsByPage(@PathVariable @Parameter(description = "Facebook page ID") String pageId) {
        List<EventDTO> events = eventService.getEventsByPageId(pageId);
        return ResponseEntity.ok(events);
    }
    
    /**
     * GET /api/events/page/{pageId}/future
     * Get FUTURE events from a specific organizer
     * 
     * Parameter: pageId = Facebook page ID
     * Example: GET /api/events/page/123456789/future
     * Returns: List of future EventDTO from that page
     * 
     * Useful for: "See all upcoming from this organizer"
     */
    @GetMapping("/page/{pageId}/future")
    public ResponseEntity<List<EventDTO>> getFutureEventsByPage(@PathVariable String pageId) {
        List<EventDTO> events = eventService.getFutureEventsByPageId(pageId);
        return ResponseEntity.ok(events);
    }
    
    /**
     * GET /api/events/place/{placeId}
     * Get all events at a SPECIFIC VENUE (place)
     * 
     * Parameter: placeId = Venue ID from database
     * Example: GET /api/events/place/s-huset-lyngby
     * Returns: List of EventDTO at that venue
     * 
     * Useful for: Find all events at a specific bar/restaurant
     */
    @GetMapping("/place/{placeId}")
    public ResponseEntity<List<EventDTO>> getEventsByPlace(@PathVariable String placeId) {
        List<EventDTO> events = eventService.getEventsByPlaceId(placeId);
        return ResponseEntity.ok(events);
    }
    
    /**
     * POST /api/events
     * CREATE a new event
     * 
     * Request body: EventDTO with event details
     * Example request:
     * {
     *   "title": "Tech Talk",
     *   "description": "JavaScript tips",
     *   "startTime": "2026-03-20T19:00:00",
     *   "pageId": "123456789"
     * }
     * 
     * Returns: Created EventDTO with ID (HTTP 201 Created)
     * 
     * Note: Usually called by the Facebook sync service, not frontend
     */
    @PostMapping
    @Operation(summary = "Create a new event", description = "Create a new event")
    @ApiResponse(responseCode = "201", description = "Event created successfully")
    public ResponseEntity<EventDTO> createEvent(@Valid @RequestBody EventDTO eventDTO) {
        EventDTO created = eventService.createEvent(eventDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    /**
     * PUT /api/events/{id}
     * UPDATE an existing event
     * 
     * Parameter: id = event ID to update
     * Request body: EventDTO with new values
     * Example: PUT /api/events/event-123
     * Body: { "title": "New Title", "startTime": "2026-03-25T20:00:00" }
     * 
     * Returns: Updated EventDTO or 404 if not found
     * Returns: HTTP 200 OK
     * 
     * Note: Only updates fields provided in request body
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an event", description = "Update an existing event")
    @ApiResponse(responseCode = "200", description = "Event updated successfully")
    @ApiResponse(responseCode = "404", description = "Event not found")
    public ResponseEntity<EventDTO> updateEvent(
            @PathVariable @Parameter(description = "Event ID") String id,
            @Valid @RequestBody EventDTO eventDTO) {
        EventDTO updated = eventService.updateEvent(id, eventDTO);
        
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(updated);
    }
    
    /**
     * POST /api/events/{id}/coverImage
     * Upload a cover image for an event
     * 
     * Parameter: id = event ID
     * File: multipart file (image file)
     * Example: POST /api/events/event-123/coverImage with image file
     * 
     * Returns: Updated EventDTO with coverImageId set
     * Returns: HTTP 200 OK
     */
    @PostMapping("/{id}/coverImage")
    @Operation(summary = "Upload cover image for event", description = "Upload a cover image for a specific event")
    @ApiResponse(responseCode = "200", description = "Cover image uploaded successfully")
    @ApiResponse(responseCode = "404", description = "Event not found")
    public ResponseEntity<EventDTO> uploadCoverImage(
            @PathVariable @Parameter(description = "Event ID") String id,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        // Verify event exists
        EventDTO eventDTO = eventService.getEventById(id);
        if (eventDTO == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Store the file
        String storedFilename = mediaService.store(file);
        MediaEntity mediaEntity = MediaEntity.builder()
                .filename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileId(storedFilename)
                .uploadedAt(Instant.now())
                .build();
        MediaEntity saved = mediaRepository.save(mediaEntity);
        
        // Update event with cover image
        eventDTO.setCoverImageId(saved.getId());
        EventDTO updated = eventService.updateEvent(id, eventDTO);
        
        return ResponseEntity.ok(updated);
    }
    
    /**
     * DELETE /api/events/{id}
     * DELETE an event
     * 
     * Parameter: id = event ID to delete
     * Example: DELETE /api/events/event-123
     * 
     * Returns: HTTP 204 No Content (success, no body)
     *          HTTP 404 Not Found (event doesn't exist)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an event", description = "Delete an event permanently")
    @ApiResponse(responseCode = "204", description = "Event deleted successfully")
    @ApiResponse(responseCode = "404", description = "Event not found")
    public ResponseEntity<Void> deleteEvent(@PathVariable @Parameter(description = "Event ID") String id) {
        boolean deleted = eventService.deleteEvent(id);
        
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.noContent().build();
    }
}
import dk.unievent.web.entity.Event;
import dk.unievent.web.entity.Page;
import dk.unievent.web.repository.EventRepository;
import dk.unievent.web.repository.PageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.*;


@RestController
@RequestMapping("/api")
/**
 * Orchestrates end-to-end event ingestion by coordinating Facebook, Secret Manager,
 * and Storage microservices while persisting normalized data in MySQL.
 */
public class EventController {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final PageRepository pageRepository;
    private final EventRepository eventRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${services.facebook.url:http://localhost:8081}")
    private String facebookServiceUrl;

    @Value("${services.secret-manager.url:http://localhost:8082}")
    private String secretManagerServiceUrl;

    @Value("${services.storage.url:http://localhost:8083}")
    private String storageServiceUrl;

    public EventController(PageRepository pageRepository,
                          EventRepository eventRepository,
                          ObjectMapper objectMapper) {
        this(pageRepository, eventRepository, objectMapper, "http://localhost:8081", "http://localhost:8082", "http://localhost:8083");
    }

    public EventController(PageRepository pageRepository,
                          EventRepository eventRepository,
                          ObjectMapper objectMapper,
                          String facebookServiceUrl,
                          String secretManagerServiceUrl,
                          String storageServiceUrl) {
        this.pageRepository = pageRepository;
        this.eventRepository = eventRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        this.facebookServiceUrl = facebookServiceUrl;
        this.secretManagerServiceUrl = secretManagerServiceUrl;
        this.storageServiceUrl = storageServiceUrl;
    }

    /**
     * Completes OAuth callback processing, stores page tokens, and registers pages locally.
     */
    @PostMapping("/callback")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> handleCallback(@RequestBody Map<String, String> input) {
        String code = input.get("code");
        boolean debug = Boolean.parseBoolean(input.get("debug"));

        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Missing code"));
        }

        try {
            // Get short-lived token from Facebook service
            Map<String, Object> shortLivedRequest = Map.of("code", code);
            ResponseEntity<Map<String, Object>> shortLivedResponse = restTemplate.exchange(
                facebookServiceUrl + "/api/facebook/oauth/token",
                HttpMethod.POST,
                new HttpEntity<>(shortLivedRequest),
                MAP_TYPE
            );

            if (!shortLivedResponse.getStatusCode().is2xxSuccessful() || shortLivedResponse.getBody() == null) {
                return ResponseEntity.status(500).body(Map.of("success", false, "error", "Failed to get short-lived token"));
            }

            String shortLivedToken = (String) shortLivedResponse.getBody().get("access_token");

            // Exchange for long-lived token
            Map<String, Object> longLivedRequest = Map.of("short_lived_token", shortLivedToken);
            ResponseEntity<Map<String, Object>> longLivedResponse = restTemplate.exchange(
                facebookServiceUrl + "/api/facebook/oauth/long-lived-token",
                HttpMethod.POST,
                new HttpEntity<>(longLivedRequest),
                MAP_TYPE
            );

            if (!longLivedResponse.getStatusCode().is2xxSuccessful() || longLivedResponse.getBody() == null) {
                return ResponseEntity.status(500).body(Map.of("success", false, "error", "Failed to get long-lived token"));
            }

            String longLivedToken = (String) longLivedResponse.getBody().get("access_token");
            Long expiresIn = ((Number) longLivedResponse.getBody().get("expires_in")).longValue();
            List<Map<String, Object>> pages = (List<Map<String, Object>>) restTemplate.getForObject(
                facebookServiceUrl + "/api/facebook/user/pages?accessToken=" + longLivedToken,
                List.class
            );

            if (pages.isEmpty()) {
                return ResponseEntity.ok(Map.of("success", true, "storedPages", 0, "message", "No pages returned."));
            }

            int storedPages = 0;
            for (Map<String, Object> pageData : pages) {
                try {
                    // Store token in Secret Manager service
                    Map<String, Object> tokenRequest = Map.of(
                        "token", pageData.get("access_token"),
                        "expiresIn", expiresIn
                    );
                    restTemplate.postForEntity(
                        secretManagerServiceUrl + "/api/secrets/pages/" + pageData.get("id") + "/token",
                        tokenRequest,
                        Map.class
                    );

                    Page page = new Page();
                    page.setId((String) pageData.get("id"));
                    page.setName((String) pageData.get("name"));
                    page.setActive(true);
                    page.setUrl("https://facebook.com/" + pageData.get("id"));
                    page.setConnectedAt(Instant.now());
                    page.setTokenRefreshedAt(Instant.now());
                    page.setTokenStoredAt(Instant.now());
                    page.setTokenExpiresAt(Instant.now().plusSeconds(expiresIn));
                    page.setTokenExpiresInDays((long) Math.ceil(expiresIn / (60.0 * 60 * 24)));
                    page.setTokenStatus("valid");
                    page.setLastRefreshSuccess(true);

                    pageRepository.save(page);
                    storedPages++;
                } catch (Exception e) {
                    // Log error
                }
            }

            return ResponseEntity.ok(Map.of("success", true, "storedPages", storedPages, "message", "Stored " + storedPages + " page token(s)."));
        } catch (Exception e) {
            String msg = e.getMessage();
            return ResponseEntity.status(500).body(Map.of("success", false, "error", msg, "message", debug ? msg : "Facebook auth failed"));
        }
    }

    /**
     * Pulls events for all connected pages and upserts them into the local event store.
     */
    @PostMapping("/ingest")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> handleIngest() {
        try {
            List<Page> pages = pageRepository.findAll();
            if (pages.isEmpty()) {
                return ResponseEntity.ok(Map.of("totalPages", 0, "totalEvents", 0, "duration", 0));
            }

            long startTime = System.currentTimeMillis();
            int totalEventsProcessed = 0;
            List<Map<String, Object>> pageResults = new ArrayList<>();

            for (Page page : pages) {
                long pageStartTime = System.currentTimeMillis();
                try {
                    // Get token from Secret Manager service
                    ResponseEntity<Map<String, Object>> tokenResponse = restTemplate.exchange(
                        secretManagerServiceUrl + "/api/secrets/pages/" + page.getId() + "/token",
                        HttpMethod.GET,
                        null,
                        MAP_TYPE
                    );

                    String token = null;
                    if (tokenResponse.getStatusCode().is2xxSuccessful() && tokenResponse.getBody() != null) {
                        token = (String) tokenResponse.getBody().get("token");
                    }

                    if (token == null) {
                        pageResults.add(Map.of(
                            "pageId", page.getId(),
                            "pageName", page.getName(),
                            "status", "skipped",
                            "reason", "no_token",
                            "duration", System.currentTimeMillis() - pageStartTime
                        ));
                        continue;
                    }

                    // Get events from Facebook service
                    List<Map<String, Object>> events = (List<Map<String, Object>>) restTemplate.getForObject(
                        facebookServiceUrl + "/api/facebook/pages/" + page.getId() + "/events?accessToken=" + token,
                        List.class
                    );
                    if (events.isEmpty()) {
                        pageResults.add(Map.of(
                            "pageId", page.getId(),
                            "pageName", page.getName(),
                            "status", "success",
                            "eventsProcessed", 0,
                            "eventsFailed", 0,
                            "duration", System.currentTimeMillis() - pageStartTime
                        ));
                        continue;
                    }

                    List<Event> eventsData = new ArrayList<>();
                    for (Map<String, Object> eventData : events) {
                        try {
                            String coverImageUrl = null;
                            Map<String, Object> cover = (Map<String, Object>) eventData.get("cover");
                            if (cover != null) {
                                Map<String, Object> source = (Map<String, Object>) cover.get("source");
                                if (source != null && source.get("source") != null) {
                                    // Call Storage service to add image from URL
                                    Map<String, Object> imageRequest = Map.of(
                                        "filePath", "covers/" + page.getId() + "/" + eventData.get("id") + ".jpg",
                                        "sourceUrl", source.get("source")
                                    );
                                    ResponseEntity<Map<String, Object>> imageResponse = restTemplate.exchange(
                                        storageServiceUrl + "/api/storage/images/from-url",
                                        HttpMethod.POST,
                                        new HttpEntity<>(imageRequest),
                                        MAP_TYPE
                                    );
                                    if (imageResponse.getStatusCode().is2xxSuccessful()) {
                                        // For now, we'll skip setting the coverImageUrl since the storage service doesn't fully implement URL downloading
                                    }
                                }
                            }

                            String placeJson = null;
                            Map<String, Object> place = (Map<String, Object>) eventData.get("place");
                            if (place != null) {
                                try {
                                    placeJson = objectMapper.writeValueAsString(place);
                                } catch (Exception e) {
                                    // If serialization fails, store as string representation
                                    placeJson = place.toString();
                                }
                            }

                            Instant eventStartTime = null;
                            Instant eventEndTime = null;
                            try {
                                String startTimeStr = (String) eventData.get("start_time");
                                if (startTimeStr != null) {
                                    eventStartTime = Instant.parse(startTimeStr);
                                }
                            } catch (Exception e) {
                                // If parsing fails, keep as null
                            }
                            try {
                                String endTimeStr = (String) eventData.get("end_time");
                                if (endTimeStr != null) {
                                    eventEndTime = Instant.parse(endTimeStr);
                                }
                            } catch (Exception e) {
                                // If parsing fails, keep as null
                            }

                            String rawJson = null;
                            try {
                                rawJson = objectMapper.writeValueAsString(eventData);
                            } catch (Exception e) {
                                // If serialization fails, skip
                            }

                            Event event = new Event();
                            event.setId((String) eventData.get("id"));
                            event.setPageId(page.getId());
                            event.setTitle((String) eventData.get("name"));
                            event.setDescription((String) eventData.get("description"));
                            event.setStartTime(eventStartTime);
                            event.setEndTime(eventEndTime);
                            event.setPlace(placeJson);
                            event.setCoverImageUrl(coverImageUrl);
                            event.setEventURL("https://facebook.com/events/" + eventData.get("id"));
                            event.setCreatedAt(Instant.now());
                            event.setUpdatedAt(Instant.now());
                            event.setRaw(rawJson);
                            eventsData.add(event);
                        } catch (Exception e) {
                            // Handle error
                        }
                    }

                    eventRepository.saveAll(eventsData);
                    totalEventsProcessed += eventsData.size();
                    pageResults.add(Map.of(
                        "pageId", page.getId(),
                        "pageName", page.getName(),
                        "status", "success",
                        "eventsProcessed", eventsData.size(),
                        "eventsFailed", events.size() - eventsData.size(),
                        "duration", System.currentTimeMillis() - pageStartTime
                    ));
                } catch (Exception e) {
                    pageResults.add(Map.of(
                        "pageId", page.getId(),
                        "pageName", page.getName(),
                        "status", "failed",
                        "error", e.getMessage(),
                        "duration", System.currentTimeMillis() - pageStartTime
                    ));
                }
            }

            return ResponseEntity.ok(Map.of(
                "totalPages", pages.size(),
                "totalEvents", totalEventsProcessed,
                "duration", System.currentTimeMillis() - startTime,
                "pageResults", pageResults
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Refreshes all stored page tokens and updates refresh metadata on each page.
     */
    @PostMapping("/refresh-tokens")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> handleRefreshTokens() {
        try {
            List<Page> pages = pageRepository.findAll();
            if (pages.isEmpty()) {
                return ResponseEntity.ok(Map.of("tokensRefreshed", 0, "tokensFailed", 0, "durationMs", 0));
            }

            long startTime = System.currentTimeMillis();
            int tokensRefreshed = 0;
            int tokensFailed = 0;

            for (Page page : pages) {
                try {
                    // Get current token from Secret Manager service
                    ResponseEntity<Map<String, Object>> tokenResponse = restTemplate.exchange(
                        secretManagerServiceUrl + "/api/secrets/pages/" + page.getId() + "/token",
                        HttpMethod.GET,
                        null,
                        MAP_TYPE
                    );

                    String currentToken = null;
                    if (tokenResponse.getStatusCode().is2xxSuccessful() && tokenResponse.getBody() != null) {
                        currentToken = (String) tokenResponse.getBody().get("token");
                    }

                    if (currentToken == null) {
                        throw new RuntimeException("No token found");
                    }

                    // Refresh token using Facebook service
                    Map<String, Object> refreshRequest = Map.of("page_token", currentToken);
                    ResponseEntity<Map<String, Object>> refreshResponse = restTemplate.exchange(
                        facebookServiceUrl + "/api/facebook/oauth/refresh",
                        HttpMethod.POST,
                        new HttpEntity<>(refreshRequest),
                        MAP_TYPE
                    );

                    if (!refreshResponse.getStatusCode().is2xxSuccessful() || refreshResponse.getBody() == null) {
                        throw new RuntimeException("Failed to refresh token");
                    }

                    String newToken = (String) refreshResponse.getBody().get("access_token");
                    Long expiresIn = ((Number) refreshResponse.getBody().get("expires_in")).longValue();

                    // Update token in Secret Manager service
                    Map<String, Object> updateRequest = Map.of(
                        "token", newToken,
                        "expiresIn", expiresIn
                    );
                    restTemplate.put(
                        secretManagerServiceUrl + "/api/secrets/pages/" + page.getId() + "/token",
                        updateRequest
                    );

                    page.setTokenRefreshedAt(Instant.now());
                    page.setLastRefreshSuccess(true);
                    page.setLastRefreshError(null);
                    pageRepository.save(page);
                    tokensRefreshed++;
                } catch (Exception e) {
                    tokensFailed++;
                    try {
                        page.setLastRefreshSuccess(false);
                        page.setLastRefreshError(e.getMessage());
                        page.setLastRefreshAttempt(Instant.now());
                        pageRepository.save(page);
                    } catch (Exception dbErr) {
                        // Silent fail
                    }
                }
            }

            return ResponseEntity.ok(Map.of(
                "tokensRefreshed", tokensRefreshed,
                "tokensFailed", tokensFailed,
                "durationMs", System.currentTimeMillis() - startTime
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
