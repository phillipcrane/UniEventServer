package dk.unievent.app.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import dk.unievent.app.application.dto.EventDTO;
import dk.unievent.app.application.service.EventService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Events", description = "Manage and retrieve events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    @Operation(summary = "Get all events", description = "Retrieve all events ordered by start time")
    @ApiResponse(responseCode = "200", description = "List of all events")
    public ResponseEntity<List<EventDTO>> getAllEvents() {
        List<EventDTO> events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/future")
    @Operation(summary = "Get future events", description = "Retrieve only upcoming events (startTime >= now)")
    @ApiResponse(responseCode = "200", description = "List of future events")
    public ResponseEntity<List<EventDTO>> getFutureEvents() {
        List<EventDTO> events = eventService.getFutureEvents();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID", description = "Retrieve a single event by its ID")
    @ApiResponse(responseCode = "200", description = "Event found")
    @ApiResponse(responseCode = "404", description = "Event not found")
    public ResponseEntity<EventDTO> getEventById(@PathVariable @Parameter(description = "Event ID") String id) {
        EventDTO event = eventService.getEventById(id);
        if (event == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(event);
    }

    @GetMapping("/page/{pageId}")
    @Operation(summary = "Get events by page", description = "Retrieve all events from a specific organizer (page)")
    @ApiResponse(responseCode = "200", description = "List of events from the page")
    public ResponseEntity<List<EventDTO>> getEventsByPage(@PathVariable @Parameter(description = "Facebook page ID") String pageId) {
        List<EventDTO> events = eventService.getEventsByPageId(pageId);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/page/{pageId}/future")
    public ResponseEntity<List<EventDTO>> getFutureEventsByPage(@PathVariable String pageId) {
        List<EventDTO> events = eventService.getFutureEventsByPageId(pageId);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/place/{placeId}")
    public ResponseEntity<List<EventDTO>> getEventsByPlace(@PathVariable String placeId) {
        List<EventDTO> events = eventService.getEventsByPlaceId(placeId);
        return ResponseEntity.ok(events);
    }

    @PostMapping
    @Operation(summary = "Create a new event", description = "Create a new event")
    @ApiResponse(responseCode = "201", description = "Event created successfully")
    public ResponseEntity<EventDTO> createEvent(@Valid @RequestBody EventDTO eventDTO) {
        EventDTO created = eventService.createEvent(eventDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

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

    @PostMapping("/{id}/coverImage")
    @Operation(summary = "Upload cover image for event", description = "Upload a cover image for a specific event")
    @ApiResponse(responseCode = "200", description = "Cover image uploaded successfully")
    @ApiResponse(responseCode = "404", description = "Event not found")
    public ResponseEntity<EventDTO> uploadCoverImage(
            @PathVariable @Parameter(description = "Event ID") String id,
            @RequestParam("file") MultipartFile file) throws IOException {
        EventDTO updated = eventService.uploadCoverImage(id, file);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

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
