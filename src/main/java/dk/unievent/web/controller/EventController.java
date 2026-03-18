package dk.unievent.web.controller;

import dk.unievent.web.dto.EventDTO;
import dk.unievent.web.service.EventService;
import dk.unievent.web.media.MediaFile;
import dk.unievent.web.media.StorageService;
import dk.unievent.web.media.MediaFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import java.io.IOException;
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
    private StorageService storageService;
    
    @Autowired
    private MediaFileRepository mediaFileRepository;
    
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
        String storedFilename = storageService.store(file);
        MediaFile mediaFile = new MediaFile(file.getOriginalFilename(), file.getContentType(), storedFilename);
        MediaFile saved = mediaFileRepository.save(mediaFile);
        
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
