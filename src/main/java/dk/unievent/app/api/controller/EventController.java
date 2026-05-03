package dk.unievent.app.api.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import dk.unievent.app.application.dto.EventDTO;
import dk.unievent.app.application.service.EventService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Optional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Manage and retrieve events")
public class EventController {

    private final EventService eventService;

    @GetMapping
    @Operation(summary = "Get all events", description = "Retrieve all events ordered by start time")
    @ApiResponse(responseCode = "200", description = "Page of all events")
    public ResponseEntity<Page<EventDTO>> getAllEvents(@PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching all events with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<EventDTO> events = eventService.getAllEvents(pageable);
        log.debug("Retrieved {} events", events.getTotalElements());
        return ResponseEntity.ok(events);
    }

    @GetMapping("/future")
    @Operation(summary = "Get future events", description = "Retrieve only upcoming events (startTime >= now)")
    @ApiResponse(responseCode = "200", description = "Page of future events")
    public ResponseEntity<Page<EventDTO>> getFutureEvents(@PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching future events with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<EventDTO> events = eventService.getFutureEvents(pageable);
        log.debug("Retrieved {} future events", events.getTotalElements());
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID", description = "Retrieve a single event by its ID")
    @ApiResponse(responseCode = "200", description = "Event found")
    @ApiResponse(responseCode = "404", description = "Event not found")
    public ResponseEntity<EventDTO> getEventById(@PathVariable @Parameter(description = "Event ID") String id) {
        log.debug("Fetching event with id: {}", id);
        Optional<EventDTO> event = eventService.getEventById(id);
        if (event.isEmpty()) {
            log.warn("Event not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
        log.debug("Event found: {}", id);
        return ResponseEntity.ok(event.get());
    }

    @GetMapping("/page/{pageId}")
    @Operation(summary = "Get events by page", description = "Retrieve all events from a specific organizer (page)")
    @ApiResponse(responseCode = "200", description = "Page of events from the page")
    public ResponseEntity<Page<EventDTO>> getEventsByPage(@PathVariable @Parameter(description = "Facebook page ID") String pageId, @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching events for page: {}", pageId);
        Page<EventDTO> events = eventService.getEventsByPageId(pageId, pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/page/{pageId}/future")
    @Operation(summary = "Get future events by page")
    public ResponseEntity<Page<EventDTO>> getFutureEventsByPage(@PathVariable String pageId, @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching future events for page: {}", pageId);
        Page<EventDTO> events = eventService.getFutureEventsByPageId(pageId, pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/place/{placeId}")
    @Operation(summary = "Get events by place")
    public ResponseEntity<Page<EventDTO>> getEventsByPlace(@PathVariable String placeId, @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching events for place: {}", placeId);
        Page<EventDTO> events = eventService.getEventsByPlaceId(placeId, pageable);
        return ResponseEntity.ok(events);
    }

    @PostMapping
    @PreAuthorize("hasRole('organizer') or hasRole('admin')")
    @RateLimiter(name = "event-create", fallbackMethod = "createFallback")
    @Operation(summary = "Create a new event", description = "Create a new event")
    @ApiResponse(responseCode = "201", description = "Event created successfully")
    public ResponseEntity<EventDTO> createEvent(@Valid @RequestBody EventDTO eventDTO) {
        log.info("Creating new event: {}", eventDTO.getTitle());
        EventDTO created = eventService.createEvent(eventDTO);
        log.info("Event created successfully with id: {}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('organizer') or hasRole('admin')")
    @RateLimiter(name = "event-update", fallbackMethod = "updateFallback")
    @Operation(summary = "Update an event", description = "Update an existing event")
    @ApiResponse(responseCode = "200", description = "Event updated successfully")
    @ApiResponse(responseCode = "404", description = "Event not found")
    public ResponseEntity<EventDTO> updateEvent(
            @PathVariable @Parameter(description = "Event ID") String id,
            @Valid @RequestBody EventDTO eventDTO) {
        log.info("Updating event with id: {}", id);
        Optional<EventDTO> updated = eventService.updateEvent(id, eventDTO);

        if (updated.isEmpty()) {
            log.warn("Event not found for update with id: {}", id);
            return ResponseEntity.notFound().build();
        }

        log.info("Event updated successfully: {}", id);
        return ResponseEntity.ok(updated.get());
    }

    @PostMapping("/{id}/coverImage")
    @PreAuthorize("hasRole('organizer') or hasRole('admin')")
    @Operation(summary = "Upload cover image for event", description = "Upload a cover image for a specific event")
    @ApiResponse(responseCode = "200", description = "Cover image uploaded successfully")
    @ApiResponse(responseCode = "404", description = "Event not found")
    public ResponseEntity<EventDTO> uploadCoverImage(
            @PathVariable @Parameter(description = "Event ID") String id,
            @RequestParam("file") MultipartFile file) throws IOException {
        log.info("Uploading cover image for event id: {}, filename: {}", id, file.getOriginalFilename());
        Optional<EventDTO> updated = eventService.uploadCoverImage(id, file);
        if (updated.isEmpty()) {
            log.warn("Event not found for cover image upload with id: {}", id);
            return ResponseEntity.notFound().build();
        }
        log.info("Cover image uploaded successfully for event: {}", id);
        return ResponseEntity.ok(updated.get());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('organizer') or hasRole('admin')")
    @RateLimiter(name = "event-delete", fallbackMethod = "deleteFallback")
    @Operation(summary = "Delete an event", description = "Delete an event permanently")
    @ApiResponse(responseCode = "204", description = "Event deleted successfully")
    @ApiResponse(responseCode = "404", description = "Event not found")
    public ResponseEntity<Void> deleteEvent(@PathVariable @Parameter(description = "Event ID") String id) {
        log.info("Deleting event with id: {}", id);
        boolean deleted = eventService.deleteEvent(id);

        if (!deleted) {
            log.warn("Event not found for deletion with id: {}", id);
            return ResponseEntity.notFound().build();
        }

        log.info("Event deleted successfully: {}", id);
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<EventDTO> createFallback(EventDTO eventDTO, Exception ex) {
        return ResponseEntity.status(429).build();
    }

    public ResponseEntity<EventDTO> updateFallback(String id, EventDTO eventDTO, Exception ex) {
        return ResponseEntity.status(429).build();
    }

    public ResponseEntity<Void> deleteFallback(String id, Exception ex) {
        return ResponseEntity.status(429).build();
    }
}
