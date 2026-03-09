package dk.unievent.web.controller;

import dk.unievent.web.dto.EventDTO;
import dk.unievent.web.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
public class EventController {
    
    @Autowired
    private EventService eventService;
    
    /**
     * GET /api/events
     * Returns ALL events ordered by start time (ascending)
     * 
     * Frontend calls this when loading the main page
     * Example response: [{ id: "evt-1", title: "Pub Night", startTime: "2026-03-15T20:00:00" }, ...]
     */
    @GetMapping
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
    public ResponseEntity<EventDTO> getEventById(@PathVariable String id) {
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
    public ResponseEntity<List<EventDTO>> getEventsByPage(@PathVariable String pageId) {
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
    public ResponseEntity<EventDTO> createEvent(@RequestBody EventDTO eventDTO) {
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
    public ResponseEntity<EventDTO> updateEvent(
            @PathVariable String id,
            @RequestBody EventDTO eventDTO) {
        EventDTO updated = eventService.updateEvent(id, eventDTO);
        
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        
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
    public ResponseEntity<Void> deleteEvent(@PathVariable String id) {
        boolean deleted = eventService.deleteEvent(id);
        
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.noContent().build();
    }
}
