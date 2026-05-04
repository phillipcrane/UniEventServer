package dk.unievent.app.api.controller;

import dk.unievent.app.api.dto.AdminIngestResponse;
import dk.unievent.app.application.service.EventService;
import dk.unievent.app.db.model.EventEntity;
import dk.unievent.app.db.repository.PageRepository;
import dk.unievent.app.infrastructure.exception.FacebookApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Manual admin trigger for Facebook event ingestion, scoped to one page.
 */
@Slf4j
@RestController
@RequestMapping("/admin/tools/ingest")
@Tag(name = "Admin Tools - Ingest", description = "Manually ingest Facebook events for one page")
public class AdminIngestController {

    private final EventService eventService;
    private final PageRepository pageRepository;

    public AdminIngestController(EventService eventService, PageRepository pageRepository) {
        this.eventService = eventService;
        this.pageRepository = pageRepository;
    }

    @PostMapping("/{pageId}")
    @Operation(summary = "Ingest events for one page", description = "Fetches events from Facebook Graph API for the given page ID and persists them.")
    public ResponseEntity<?> ingest(@PathVariable String pageId) {
        log.info("Received ingest request for page: {}", pageId);
        if (!pageRepository.existsById(pageId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("pageId", pageId, "error", "Page not found"));
        }

        try {
            List<EventEntity> events = eventService.ingestFacebookEvents(pageId);
            List<String> titles = events.stream().map(EventEntity::getTitle).toList();
            return ResponseEntity.ok(new AdminIngestResponse(pageId, events.size(), titles));
        } catch (FacebookApiException e) {
            log.error("Facebook API error during manual ingest for page: {}", pageId, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "pageId", pageId,
                "errorType", e.getErrorType(),
                "statusCode", e.getStatusCode(),
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected error during manual ingest for page: {}", pageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("pageId", pageId, "message", e.getMessage()));
        }
    }
}
